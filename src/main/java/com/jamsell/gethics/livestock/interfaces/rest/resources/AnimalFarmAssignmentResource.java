package com.jamsell.gethics.livestock.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

/** {@code fromFarmId} es null en la primera asignacion del animal. */
public record AnimalFarmAssignmentResource(UUID fromFarmId, UUID toFarmId, Instant assignedAt) {
}
