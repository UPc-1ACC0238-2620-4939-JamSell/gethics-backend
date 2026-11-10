package com.jamsell.gethics.sanitary.domain.model.commands;

import java.util.UUID;

/** US-33: cancela un evento programado del historial clinico del animal; la accion no lleva datos adicionales. */
public record CancelScheduledEventCommand(UUID animalId, UUID eventId) {
}
