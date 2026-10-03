package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduledEventResource(
        UUID id,
        UUID animalId,
        SanitaryEventType type,
        LocalDate scheduledDate,
        String description,
        SanitaryEventStatus status) {
}
