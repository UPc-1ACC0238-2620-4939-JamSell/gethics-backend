package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.AssignAnimalToFarmCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AnimalFarmCommandServiceImpl implements AnimalFarmCommandService {

    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final AnimalFarmAssignmentRepository assignments;
    private final Clock clock;

    public AnimalFarmCommandServiceImpl(AnimalRepository animals, FarmRepository farms,
                                        AnimalFarmAssignmentRepository assignments, Clock clock) {
        this.animals = animals;
        this.farms = farms;
        this.assignments = assignments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Animal handle(AssignAnimalToFarmCommand command) {
        var animal = animals.findById(command.animalId()).orElseThrow(AnimalNotFoundException::new);
        if (!farms.existsById(command.farmId())) {
            throw new FarmNotFoundException();
        }
        var previousFarmId = animal.getFarmId();
        if (!animal.assignToFarm(command.farmId())) {
            return animal;
        }
        // Animal e historial se guardan en la misma transaccion: nunca queda un cambio sin su registro.
        var saved = animals.save(animal);
        assignments.save(AnimalFarmAssignment.record(saved.getId(), previousFarmId, command.farmId(), Instant.now(clock)));
        return saved;
    }
}
