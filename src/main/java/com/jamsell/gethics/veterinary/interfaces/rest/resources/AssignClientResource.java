package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignClientResource(
        @NotNull(message = "Veterinarian id is required") UUID veterinarianId,
        @NotNull(message = "Client id is required") UUID clientId) {
}
