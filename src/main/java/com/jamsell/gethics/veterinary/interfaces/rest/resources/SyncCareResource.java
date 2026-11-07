package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SyncCareResource(
        @NotEmpty(message = "At least one item is required")
        @Size(max = 100, message = "A batch can have at most 100 items")
        List<@Valid SyncCareItemResource> items) {
}
