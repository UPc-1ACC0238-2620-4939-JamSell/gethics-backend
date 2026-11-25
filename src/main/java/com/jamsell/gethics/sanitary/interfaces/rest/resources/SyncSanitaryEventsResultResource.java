package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record SyncSanitaryEventsResultResource(
        int totalReceived,
        int syncedCount,
        int failedCount,
        List<ItemSyncResult> results
) {

    public record ItemSyncResult(
            String clientEventId,
            UUID serverEventId,
            String status,
            String message
    ) {
    }
}
