package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.application.internal.outboundservices.SaleIncomeRegistrar;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalSaleException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.DeactivateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.SellAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.model.events.AnimalLeftActiveHerdEvent;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSale;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class AnimalCommandServiceImpl implements AnimalCommandService {

    private final AnimalRepository repository;
    private final FarmRepository farms;
    private final AnimalFarmAssignmentRepository assignments;
    // Mismo Clock (zona configurada) que usa sanitary: "hoy" coincide entre contextos.
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final SaleIncomeRegistrar saleIncome;

    public AnimalCommandServiceImpl(AnimalRepository repository, FarmRepository farms,
                                    AnimalFarmAssignmentRepository assignments, Clock clock,
                                    ApplicationEventPublisher events, SaleIncomeRegistrar saleIncome) {
        this.repository = repository;
        this.farms = farms;
        this.assignments = assignments;
        this.clock = clock;
        this.events = events;
        this.saleIncome = saleIncome;
    }

    @Override
    @Transactional
    public Animal handle(RegisterAnimalCommand command) {
        var animal = Animal.register(command, LocalDate.now(clock));
        if (animal.getFarmId() != null && !farms.existsById(animal.getFarmId())) {
            throw new FarmNotFoundException();
        }
        if (repository.existsByTag(animal.getTag())) {
            throw new DuplicateAnimalTagException(animal.getTag());
        }
        Animal saved;
        try {
            saved = repository.save(animal);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultaneos con el mismo arete: gana la restriccion unica de la tabla.
            throw new DuplicateAnimalTagException(animal.getTag());
        }
        if (saved.getFarmId() != null) {
            // Un animal registrado ya en una granja deja su primera asignacion en el historial (US-10).
            assignments.save(AnimalFarmAssignment.record(saved.getId(), null, saved.getFarmId(), Instant.now(clock)));
        }
        return saved;
    }

    /** US-07, Escenario 1: carga el animal, aplica los cambios y persiste. Solo un animal ACTIVE se puede editar. */
    @Override
    @Transactional
    public Animal handle(UpdateAnimalCommand command) {
        var animal = repository.findById(command.animalId())
                .orElseThrow(() -> new AnimalNotFoundException(command.animalId()));
        animal.updateDetails(command, LocalDate.now(clock));
        // Si el formulario trae otra granja, el cambio sigue la regla de US-10: la granja debe existir, el animal no
        // puede quedarse sin granja (null = sin cambio) y el movimiento queda en el historial.
        var previousFarmId = animal.getFarmId();
        var newFarmId = command.farmId();
        boolean moved = false;
        if (newFarmId != null && !newFarmId.equals(previousFarmId)) {
            if (!farms.existsById(newFarmId)) {
                throw new FarmNotFoundException();
            }
            moved = animal.assignToFarm(newFarmId);
        }
        var saved = repository.save(animal);
        if (moved) {
            assignments.save(AnimalFarmAssignment.record(saved.getId(), previousFarmId, newFarmId, Instant.now(clock)));
        }
        return saved;
    }

    /**
     * US-08: baja logica. Cambia el estado a INACTIVE; no borra nada, asi el historial sanitario se conserva. Avisa con
     * un evento para que los demas contextos dejen de tratar al animal como activo (p. ej. recordatorios).
     */
    @Override
    @Transactional
    public Animal handle(DeactivateAnimalCommand command) {
        var animal = repository.findById(command.animalId())
                .orElseThrow(() -> new AnimalNotFoundException(command.animalId()));
        animal.deactivate();
        var saved = repository.save(animal);
        events.publishEvent(new AnimalLeftActiveHerdEvent(saved.getId(), saved.getStatus()));
        return saved;
    }

    /**
     * US-30: venta. En una sola transaccion el animal pasa a SOLD (conserva su historial) y se registra el ingreso
     * en Finance; si el ingreso falla, la venta no se guarda.
     */
    @Override
    @Transactional
    public AnimalSale handle(SellAnimalCommand command) {
        var animal = repository.findById(command.animalId())
                .orElseThrow(() -> new AnimalNotFoundException(command.animalId()));
        var today = LocalDate.now(clock);
        animal.sell(command.price(), command.saleDate(), today);
        var ownerId = resolveOwner(animal, command.ownerId());
        var saved = repository.save(animal);
        var incomeId = saleIncome.registerIncome(ownerId, command.price(), command.saleDate(),
                "Venta del animal " + saved.getTag());
        events.publishEvent(new AnimalLeftActiveHerdEvent(saved.getId(), saved.getStatus()));
        return new AnimalSale(saved, incomeId);
    }

    /** El dueno es el de la granja del animal; sin granja, el que informe la solicitud. */
    private UUID resolveOwner(Animal animal, UUID requestedOwnerId) {
        if (animal.getFarmId() != null) {
            return farms.findById(animal.getFarmId()).map(Farm::getOwnerId)
                    .orElseThrow(FarmNotFoundException::new);
        }
        if (requestedOwnerId == null) {
            throw new InvalidAnimalSaleException(
                    "El animal no tiene granja: indica el dueno (ownerId) para registrar el ingreso.");
        }
        return requestedOwnerId;
    }
}
