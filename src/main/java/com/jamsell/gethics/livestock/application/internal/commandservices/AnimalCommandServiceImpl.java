package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class AnimalCommandServiceImpl implements AnimalCommandService {

    private final AnimalRepository repository;
    // Mismo Clock (zona configurada) que usa sanitary: "hoy" coincide entre contextos.
    private final Clock clock;

    public AnimalCommandServiceImpl(AnimalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Animal handle(RegisterAnimalCommand command) {
        var animal = Animal.register(command, LocalDate.now(clock));
        if (repository.existsByTag(animal.getTag())) {
            throw new DuplicateAnimalTagException(animal.getTag());
        }
        try {
            return repository.save(animal);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultaneos con el mismo arete: gana la restriccion unica de la tabla.
            throw new DuplicateAnimalTagException(animal.getTag());
        }
    }
}
