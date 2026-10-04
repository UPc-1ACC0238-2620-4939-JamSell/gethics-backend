package com.jamsell.gethics.sanitary.domain.model.commands;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompleteScheduledEventCommand(
        UUID animalId,
        UUID eventId,
        LocalDateTime occurredAt,
        String description) {
}
