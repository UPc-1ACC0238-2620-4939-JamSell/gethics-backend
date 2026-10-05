package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
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
}
