package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClinicalHistoryEventResource(
        UUID id,
        SanitaryEventType type,
        SanitaryEventStatus status,
        LocalDateTime occurredAt,
        LocalDate scheduledDate,
        String description) {
}
