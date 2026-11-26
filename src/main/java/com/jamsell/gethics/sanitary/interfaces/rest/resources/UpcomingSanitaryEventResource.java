package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDate;
import java.util.UUID;

/** US-46: {@code daysUntil} es negativo si el evento ya vencio; {@code overdue} lo indica de forma explicita. */
public record UpcomingSanitaryEventResource(
        UUID id,
        UUID animalId,
        SanitaryEventType type,
        LocalDate scheduledDate,
        String description,
        long daysUntil,
        boolean overdue) {
}
