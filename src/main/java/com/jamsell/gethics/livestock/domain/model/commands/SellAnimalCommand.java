package com.jamsell.gethics.livestock.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * US-30: venta de un animal. {@code ownerId} es opcional: si el animal tiene granja, el dueno es el de la granja;
 * si no, hay que informarlo para registrar el ingreso.
 */
public record SellAnimalCommand(UUID animalId, UUID ownerId, BigDecimal price, LocalDate saleDate) {
}
