package com.jamsell.gethics.livestock.domain.model.events;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

import java.util.UUID;

/**
 * El animal dejo de estar ACTIVE (baja US-08 o venta US-30). Otros contextos reaccionan a este evento: por ejemplo,
 * Sanitary cancela sus eventos programados para que no sigan generando recordatorios.
 */
public record AnimalLeftActiveHerdEvent(UUID animalId, AnimalStatus newStatus) {
}
