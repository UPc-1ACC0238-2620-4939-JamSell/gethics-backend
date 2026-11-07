package com.jamsell.gethics.veterinary.domain.model.valueobjects;

import java.util.UUID;

public record CareSyncOutcome(UUID clientRequestId, CareSyncStatus status, String message, UUID careRecordId) {
}
