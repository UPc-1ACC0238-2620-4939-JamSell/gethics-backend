package com.jamsell.gethics.livestock.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterFarmResource(
        @NotNull(message = "El dueno de la granja es obligatorio.") UUID ownerId,
        @NotBlank(message = "El nombre de la granja es obligatorio.")
        @Size(max = 100, message = "El nombre de la granja no puede superar 100 caracteres.") String name,
        @NotBlank(message = "La ubicacion es obligatoria.")
        @Size(max = 200, message = "La ubicacion no puede superar 200 caracteres.") String location,
        @DecimalMin(value = "0", inclusive = false, message = "El tamano de la granja debe ser mayor a 0.")
        @Digits(integer = 7, fraction = 2, message = "El tamano admite hasta 7 enteros y 2 decimales.")
        BigDecimal sizeHectares) {
}
