package com.jamsell.gethics.livestock.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

/** {@code message} solo trae valor cuando {@code assignments} esta vacio ("Sin cambios de granja."). */
public record AnimalFarmHistoryResource(UUID animalId, List<AnimalFarmAssignmentResource> assignments, String message) {
}
