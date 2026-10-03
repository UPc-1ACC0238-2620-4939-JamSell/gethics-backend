package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record ClinicalHistoryResource(UUID animalId, List<ClinicalHistoryEventResource> events, String message) {
}
