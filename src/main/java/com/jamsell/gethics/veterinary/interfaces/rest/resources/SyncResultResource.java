package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncStatus;
import java.util.UUID;

public record SyncResultResource(UUID clientRequestId, CareSyncStatus status, String message, UUID careRecordId) {
}
