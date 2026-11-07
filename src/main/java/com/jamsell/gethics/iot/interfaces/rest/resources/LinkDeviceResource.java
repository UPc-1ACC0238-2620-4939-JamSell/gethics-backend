package com.jamsell.gethics.iot.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LinkDeviceResource(
        @NotNull(message = "El ownerId es obligatorio.") UUID ownerId,
        @NotBlank(message = "El codigo del dispositivo es obligatorio.")
        @Size(max = 50, message = "El codigo del dispositivo no puede superar 50 caracteres.") String code) {
}
