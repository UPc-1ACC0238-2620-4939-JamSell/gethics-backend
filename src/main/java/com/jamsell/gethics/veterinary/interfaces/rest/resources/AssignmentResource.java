package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResource(
        UUID id,
        UUID veterinarianId,
        UUID clientId,
        Instant assignedAt,
        VeterinaryAssignmentStatus status) {
}
