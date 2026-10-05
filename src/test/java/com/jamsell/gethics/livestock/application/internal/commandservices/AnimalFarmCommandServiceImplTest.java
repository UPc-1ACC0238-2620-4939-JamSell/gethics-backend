package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.AssignAnimalToFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnimalFarmCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2042-06-07T13:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Lima"));

    private final AnimalRepository animals = mock(AnimalRepository.class);
    private final FarmRepository farms = mock(FarmRepository.class);
    private final AnimalFarmAssignmentRepository assignments = mock(AnimalFarmAssignmentRepository.class);
    private final AnimalFarmCommandServiceImpl service = new AnimalFarmCommandServiceImpl(animals, farms, assignments, CLOCK);

    private final UUID animalId = UUID.randomUUID();
    private final UUID farmA = UUID.randomUUID();
    private final UUID farmB = UUID.randomUUID();

    private Animal animal() {
        var animal = Animal.register(new RegisterAnimalCommand("MX-1", null, "Holstein", null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(animal, "id", animalId);
        return animal;
    }

    private void givenAnimalAndFarms(Animal animal) {
        when(animals.findById(animalId)).thenReturn(Optional.of(animal));
        when(farms.existsById(farmA)).thenReturn(true);
        when(farms.existsById(farmB)).thenReturn(true);
        when(animals.save(any(Animal.class))).thenAnswer(i -> i.getArgument(0));
    }

    private AnimalFarmAssignment savedAssignment() {
        var captor = ArgumentCaptor.forClass(AnimalFarmAssignment.class);
        verify(assignments).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void firstAssignmentSavesTheAnimalAndRecordsAChangeWithoutOrigin() {
        givenAnimalAndFarms(animal());

        var result = service.handle(new AssignAnimalToFarmCommand(animalId, farmA));

        assertEquals(farmA, result.getFarmId());
        var assignment = savedAssignment();
        assertEquals(animalId, assignment.getAnimalId());
        assertNull(assignment.getFromFarmId());
        assertEquals(farmA, assignment.getToFarmId());
        assertEquals(NOW, assignment.getAssignedAt());
    }

    @Test
    void movingToAnotherFarmKeepsTheHistoryOfTheChange() {
        var animal = animal();
        animal.assignToFarm(farmA);
        givenAnimalAndFarms(animal);

        var result = service.handle(new AssignAnimalToFarmCommand(animalId, farmB));

        assertEquals(farmB, result.getFarmId());
        var assignment = savedAssignment();
        assertEquals(farmA, assignment.getFromFarmId());
        assertEquals(farmB, assignment.getToFarmId());
    }

    @Test
    void assigningTheSameFarmAgainSavesNothingAndRecordsNoChange() {
        var animal = animal();
        animal.assignToFarm(farmA);
        givenAnimalAndFarms(animal);

        var result = service.handle(new AssignAnimalToFarmCommand(animalId, farmA));

        assertEquals(farmA, result.getFarmId());
        verify(animals, never()).save(any());
        verifyNoInteractions(assignments);
    }

    @Test
    void unknownAnimalFailsWithoutTouchingAnything() {
        when(animals.findById(animalId)).thenReturn(Optional.empty());

        assertThrows(AnimalNotFoundException.class, () -> service.handle(new AssignAnimalToFarmCommand(animalId, farmA)));

        verify(animals, never()).save(any());
        verifyNoInteractions(assignments);
    }

    @Test
    void unknownFarmFailsAndTheAnimalKeepsItsFarm() {
        var animal = animal();
        animal.assignToFarm(farmA);
        when(animals.findById(animalId)).thenReturn(Optional.of(animal));
        when(farms.existsById(farmB)).thenReturn(false);

        assertThrows(FarmNotFoundException.class, () -> service.handle(new AssignAnimalToFarmCommand(animalId, farmB)));

        assertEquals(farmA, animal.getFarmId());
        verify(animals, never()).save(any());
        verifyNoInteractions(assignments);
    }
}
