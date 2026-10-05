package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterCareResource(
        @NotNull(message = "Client request id is required") UUID clientRequestId,
        @NotNull(message = "Veterinarian id is required") UUID veterinarianId,
        @NotBlank(message = "Diagnosis is required") String diagnosis,
        @NotBlank(message = "Treatment is required") String treatment,
        LocalDate nextControlDate,
        Instant occurredAt) {
}
