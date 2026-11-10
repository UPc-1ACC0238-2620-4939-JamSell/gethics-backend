package com.jamsell.gethics.sanitary.domain.model.commands;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDate;
import java.util.UUID;

/** US-35: {@code clientRequestId} es opcional; con el, la programacion es idempotente. */
public record ScheduleSanitaryEventCommand(
        UUID animalId,
        SanitaryEventType type,
        LocalDate scheduledDate,
        String description,
        UUID clientRequestId) {

    /** Clientes sin idempotencia (anteriores a US-35). */
    public ScheduleSanitaryEventCommand(UUID animalId, SanitaryEventType type, LocalDate scheduledDate, String description) {
        this(animalId, type, scheduledDate, description, null);
    }
}
