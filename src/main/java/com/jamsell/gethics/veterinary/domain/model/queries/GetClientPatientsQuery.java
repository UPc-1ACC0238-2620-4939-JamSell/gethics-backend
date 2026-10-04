package com.jamsell.gethics.veterinary.domain.model.queries;

import java.util.UUID;

public record GetClientPatientsQuery(UUID veterinarianId, UUID clientId) {
}
