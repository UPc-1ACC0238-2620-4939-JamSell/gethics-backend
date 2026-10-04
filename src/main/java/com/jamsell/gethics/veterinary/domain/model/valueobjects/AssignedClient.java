package com.jamsell.gethics.veterinary.domain.model.valueobjects;

import java.time.Instant;
import java.util.UUID;

public record AssignedClient(UUID assignmentId, UUID clientId, String location, Instant assignedAt) {
}
