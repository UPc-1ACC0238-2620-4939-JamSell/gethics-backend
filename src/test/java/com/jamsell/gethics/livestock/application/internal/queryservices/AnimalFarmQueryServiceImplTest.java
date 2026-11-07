package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalFarmHistoryQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnimalFarmQueryServiceImplTest {

    private final AnimalRepository animals = mock(AnimalRepository.class);
    private final FarmRepository farms = mock(FarmRepository.class);
    private final AnimalQueryRepository animalQueries = mock(AnimalQueryRepository.class);
    private final AnimalFarmAssignmentRepository assignments = mock(AnimalFarmAssignmentRepository.class);
    private final AnimalFarmQueryServiceImpl service = new AnimalFarmQueryServiceImpl(animals, farms, animalQueries, assignments);

    private final UUID animalId = UUID.randomUUID();
    private final UUID farmId = UUID.randomUUID();

    @Test
    void returnsTheHistoryOfAnExistingAnimal() {
        var history = List.of(AnimalFarmAssignment.record(animalId, null, farmId, Instant.now()));
        when(animals.existsById(animalId)).thenReturn(true);
        when(assignments.findByAnimalId(animalId)).thenReturn(history);

        assertEquals(history, service.handle(new GetAnimalFarmHistoryQuery(animalId)));
    }

    @Test
    void anAnimalThatNeverHadAFarmHasAnEmptyHistory() {
        when(animals.existsById(animalId)).thenReturn(true);
        when(assignments.findByAnimalId(animalId)).thenReturn(List.of());

        assertTrue(service.handle(new GetAnimalFarmHistoryQuery(animalId)).isEmpty());
    }

    @Test
    void historyOfAnUnknownAnimalFails() {
        when(animals.existsById(animalId)).thenReturn(false);

        assertThrows(AnimalNotFoundException.class, () -> service.handle(new GetAnimalFarmHistoryQuery(animalId)));

        verifyNoInteractions(assignments);
    }

    @Test
    void returnsTheAnimalsOfAnExistingFarm() {
        var found = List.of(Animal.register(
                new RegisterAnimalCommand("MX-1", null, "Holstein", null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1)));
        when(farms.existsById(farmId)).thenReturn(true);
        when(animalQueries.findByFarmId(farmId, AnimalStatus.SOLD)).thenReturn(found);

        assertEquals(found, service.handle(GetAnimalsByFarmQuery.of(farmId, AnimalStatus.SOLD)));
    }

    @Test
    void listingAnUnknownFarmFails() {
        when(farms.existsById(farmId)).thenReturn(false);

        assertThrows(FarmNotFoundException.class, () -> service.handle(GetAnimalsByFarmQuery.of(farmId, null)));

        verifyNoInteractions(animalQueries);
    }

    @Test
    void theFarmQueryDefaultsToActiveAnimals() {
        assertEquals(AnimalStatus.ACTIVE, GetAnimalsByFarmQuery.of(farmId, null).status());
        assertEquals(AnimalStatus.INACTIVE, GetAnimalsByFarmQuery.of(farmId, AnimalStatus.INACTIVE).status());
    }
}
