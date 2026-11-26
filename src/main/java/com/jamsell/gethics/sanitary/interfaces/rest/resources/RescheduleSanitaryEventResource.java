package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** US-47: {@code description} es opcional; si no se envia, el evento conserva sus notas. */
public record RescheduleSanitaryEventResource(
        @NotNull(message = "La fecha programada es obligatoria.")
        @FutureOrPresent(message = "La fecha programada no puede ser anterior a hoy.") LocalDate scheduledDate,
        @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres.") String description) {
}
