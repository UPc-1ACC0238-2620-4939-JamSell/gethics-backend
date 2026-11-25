package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SyncSanitaryEventsResource(
        @NotEmpty List<@Valid OfflineSanitaryEventItemResource> events
) {

    public record OfflineSanitaryEventItemResource(
            @NotBlank String clientEventId,
            UUID animalId,
            @NotBlank String eventType,
            String description,
            @NotNull LocalDateTime eventDate
    ) {
    }
}
