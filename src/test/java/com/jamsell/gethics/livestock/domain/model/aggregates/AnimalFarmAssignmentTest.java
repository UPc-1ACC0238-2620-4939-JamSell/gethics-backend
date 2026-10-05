package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnimalFarmAssignmentTest {

    private final UUID farmA = UUID.randomUUID();
    private final UUID farmB = UUID.randomUUID();

    private Animal animal() {
        return Animal.register(new RegisterAnimalCommand("MX-1", null, "Holstein", null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
    }

    @Test
    void aNewAnimalHasNoFarm() {
        assertNull(animal().getFarmId());
    }

    @Test
    void firstAssignmentSetsTheFarm() {
        var animal = animal();

        assertTrue(animal.assignToFarm(farmA));
        assertEquals(farmA, animal.getFarmId());
    }

    @Test
    void movingToAnotherFarmReplacesIt() {
        var animal = animal();
        animal.assignToFarm(farmA);

        assertTrue(animal.assignToFarm(farmB));
        assertEquals(farmB, animal.getFarmId());
    }

    @Test
    void assigningTheSameFarmChangesNothing() {
        var animal = animal();
        animal.assignToFarm(farmA);

        assertFalse(animal.assignToFarm(farmA));
        assertEquals(farmA, animal.getFarmId());
    }

    @Test
    void anAnimalCannotBeLeftWithoutAFarm() {
        var animal = animal();
        animal.assignToFarm(farmA);

        assertThrows(NullPointerException.class, () -> animal.assignToFarm(null));
        assertEquals(farmA, animal.getFarmId());
    }

    @Test
    void recordsTheChangeWithItsOriginAndDestination() {
        var animalId = UUID.randomUUID();
        var at = Instant.parse("2042-06-07T13:00:00Z");

        var assignment = AnimalFarmAssignment.record(animalId, farmA, farmB, at);

        assertEquals(animalId, assignment.getAnimalId());
        assertEquals(farmA, assignment.getFromFarmId());
        assertEquals(farmB, assignment.getToFarmId());
        assertEquals(at, assignment.getAssignedAt());
    }

    @Test
    void theFirstAssignmentHasNoOrigin() {
        var assignment = AnimalFarmAssignment.record(UUID.randomUUID(), null, farmA, Instant.now());

        assertNull(assignment.getFromFarmId());
    }

    @Test
    void aChangeNeedsAnAnimalADestinationAndATime() {
        var now = Instant.now();

        assertThrows(NullPointerException.class, () -> AnimalFarmAssignment.record(null, farmA, farmB, now));
        assertThrows(NullPointerException.class, () -> AnimalFarmAssignment.record(UUID.randomUUID(), farmA, null, now));
        assertThrows(NullPointerException.class, () -> AnimalFarmAssignment.record(UUID.randomUUID(), farmA, farmB, null));
    }
}
