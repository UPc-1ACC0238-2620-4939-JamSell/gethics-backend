package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.DeactivateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class AnimalCommandServiceImpl implements AnimalCommandService {

    private final AnimalRepository repository;
    private final FarmRepository farms;
    private final AnimalFarmAssignmentRepository assignments;
    // Mismo Clock (zona configurada) que usa sanitary: "hoy" coincide entre contextos.
    private final Clock clock;

    public AnimalCommandServiceImpl(AnimalRepository repository, FarmRepository farms,
                                    AnimalFarmAssignmentRepository assignments, Clock clock) {
        this.repository = repository;
        this.farms = farms;
        this.assignments = assignments;
        this.clock = clock;
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

    /** US-07, Escenario 1: carga el animal, aplica los cambios y persiste. */
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

    /** US-08: baja logica. Cambia el estado a INACTIVE; no borra nada, asi el historial sanitario se conserva. */
    @Override
    @Transactional
    public Animal handle(DeactivateAnimalCommand command) {
        var animal = repository.findById(command.animalId())
                .orElseThrow(() -> new AnimalNotFoundException(command.animalId()));
        animal.deactivate();
        return repository.save(animal);
    }
}
