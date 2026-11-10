package com.jamsell.gethics.sanitary.domain.model.commands;

import java.util.UUID;

/** Cancela todos los eventos programados de un animal que dejo de estar activo (baja o venta). */
public record CancelAllScheduledEventsCommand(UUID animalId) {
}
