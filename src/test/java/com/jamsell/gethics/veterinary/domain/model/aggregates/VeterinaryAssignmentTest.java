package com.jamsell.gethics.veterinary.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VeterinaryAssignmentTest {

    @Test
    void newAssignmentIsActive() {
        var assignment = new VeterinaryAssignment(UUID.randomUUID(), UUID.randomUUID());

        assertTrue(assignment.isActive());
        assertEquals(VeterinaryAssignmentStatus.ACTIVE, assignment.getStatus());
    }

    @Test
    void deactivateChangesStatus() {
        var assignment = new VeterinaryAssignment(UUID.randomUUID(), UUID.randomUUID());

        assignment.deactivate();

        assertEquals(VeterinaryAssignmentStatus.INACTIVE, assignment.getStatus());
    }

    @Test
    void sameVeterinarianAndClientThrows() {
        var id = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> new VeterinaryAssignment(id, id));
    }

    @Test
    void nullIdsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new VeterinaryAssignment(null, UUID.randomUUID()));
    }
}