package com.jamsell.gethics.iot.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code recordedAt} es opcional: si no viene, se usa la hora de recepcion del servidor. */
public record RegisterSensorReadingResource(
        @NotBlank(message = "El metric de la lectura es obligatorio.")
        @Size(max = 50, message = "El metric de la lectura no puede superar 50 caracteres.") String metric,
        @NotNull(message = "El valor de la lectura es obligatorio.") BigDecimal value,
        LocalDateTime recordedAt) {
}
