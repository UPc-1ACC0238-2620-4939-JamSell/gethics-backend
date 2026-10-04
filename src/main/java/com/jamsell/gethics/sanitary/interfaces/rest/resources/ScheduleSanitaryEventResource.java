package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ScheduleSanitaryEventResource(
        @NotNull(message = "El tipo de evento es obligatorio.") SanitaryEventType type,
        @NotNull(message = "La fecha programada es obligatoria.")
        @FutureOrPresent(message = "La fecha programada no puede ser anterior a hoy.") LocalDate scheduledDate,
        @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres.") String description) {
}
