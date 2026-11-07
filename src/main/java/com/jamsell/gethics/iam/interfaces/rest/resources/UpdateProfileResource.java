package com.jamsell.gethics.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileResource(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 25) String phone
) {
}
