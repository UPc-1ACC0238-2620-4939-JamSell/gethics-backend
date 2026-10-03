package com.jamsell.gethics.sanitary.domain.model.commands;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterSanitaryEventCommand(
        UUID animalId,
        SanitaryEventType type,
        LocalDateTime occurredAt,
        String description) {
}
