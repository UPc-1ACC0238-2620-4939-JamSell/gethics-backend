package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** US-35: {@code clientRequestId} es opcional; si se envia, repetir el request devuelve el mismo evento (200). */
public record RegisterSanitaryEventResource(
        @NotNull(message = "El tipo de evento es obligatorio.") SanitaryEventType type,
        @NotNull(message = "La fecha del evento es obligatoria.") LocalDateTime occurredAt,
        @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres.") String description,
        UUID clientRequestId) {
}
