package com.jamsell.gethics.sanitary.domain.model.commands;

import java.time.LocalDateTime;
import java.util.UUID;

public record SyncOfflineSanitaryEventItem(
        String clientEventId,
        UUID animalId,
        String eventType,
        String description,
        LocalDateTime eventDate
) {
}
