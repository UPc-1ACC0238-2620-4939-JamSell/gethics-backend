package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SyncCareItemResource(
        @NotNull(message = "Patient id is required") UUID patientId,
        @NotNull(message = "Care data is required") @Valid RegisterCareResource care) {
}
