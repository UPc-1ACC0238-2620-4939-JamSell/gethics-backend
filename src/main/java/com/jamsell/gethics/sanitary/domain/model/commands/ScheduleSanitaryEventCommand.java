package com.jamsell.gethics.sanitary.domain.model.commands;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDate;
import java.util.UUID;

public record ScheduleSanitaryEventCommand(
        UUID animalId,
        SanitaryEventType type,
        LocalDate scheduledDate,
        String description) {
}
