package com.jamsell.gethics.livestock.interfaces.rest.resources;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** El arete y el codigo QR no se incluyen: son identidad del animal y no se editan desde este formulario (US-07). */
public record UpdateAnimalResource(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres.") String name,
        @NotBlank(message = "La raza es obligatoria.")
        @Size(max = 60, message = "La raza no puede superar 60 caracteres.") String breed,
        AnimalSex sex,
        @NotNull(message = "La fecha de nacimiento es obligatoria.") LocalDate birthDate,
        @DecimalMin(value = "0", inclusive = false, message = "El peso inicial debe ser mayor a 0.")
        @Digits(integer = 5, fraction = 2, message = "El peso inicial admite hasta 5 enteros y 2 decimales.")
        BigDecimal initialWeightKg,
        @Size(max = 500, message = "La URL de la foto no puede superar 500 caracteres.") String photoUrl,
        UUID farmId) {
}
