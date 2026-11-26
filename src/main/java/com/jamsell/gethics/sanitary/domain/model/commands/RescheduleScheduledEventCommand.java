package com.jamsell.gethics.sanitary.domain.model.commands;

import java.time.LocalDate;
import java.util.UUID;

/** US-47: nueva fecha (y opcionalmente nuevas notas) de un evento programado del historial clinico del animal. */
public record RescheduleScheduledEventCommand(
        UUID animalId,
        UUID eventId,
        LocalDate scheduledDate,
        String description) {
}
