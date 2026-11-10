package com.jamsell.gethics.livestock.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** US-30. {@code ownerId} solo hace falta si el animal no tiene granja (el dueno sale de la granja). */
public record SellAnimalResource(
        @NotNull(message = "El precio de venta es obligatorio.")
        @DecimalMin(value = "0", inclusive = false, message = "El precio de venta debe ser mayor a 0.")
        @Digits(integer = 10, fraction = 2, message = "El precio admite hasta 10 enteros y 2 decimales.")
        BigDecimal price,
        @NotNull(message = "La fecha de venta es obligatoria.") LocalDate saleDate,
        UUID ownerId) {
}
