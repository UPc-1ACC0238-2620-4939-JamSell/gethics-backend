package com.jamsell.gethics.sanitary.domain.model.commands;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/** US-35: {@code clientRequestId} es opcional; con el, el registro es idempotente. */
public record RegisterSanitaryEventCommand(
        UUID animalId,
        SanitaryEventType type,
        LocalDateTime occurredAt,
        String description,
        UUID clientRequestId) {

    /** Clientes sin idempotencia (anteriores a US-35). */
    public RegisterSanitaryEventCommand(UUID animalId, SanitaryEventType type, LocalDateTime occurredAt, String description) {
        this(animalId, type, occurredAt, description, null);
    }
}
