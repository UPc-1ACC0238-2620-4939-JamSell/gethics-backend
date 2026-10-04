package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CompleteSanitaryEventResource(
        @NotNull(message = "La fecha de aplicacion es obligatoria.") LocalDateTime occurredAt,
        @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres.") String description) {
}
