package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record AssignedClientResource(UUID assignmentId, UUID clientId, String location, Instant assignedAt) {
}

