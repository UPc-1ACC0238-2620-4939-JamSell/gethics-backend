package com.jamsell.gethics.veterinary.domain.model.valueobjects;

import java.util.UUID;

/** {@code clientId} es null si el animal no tiene granja (no se puede saber de que cliente es). */
public record PatientAccess(UUID patientId, boolean active, UUID clientId) {
}
