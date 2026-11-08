package com.jamsell.gethics.iam.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record LoginResource(@NotBlank String email, @NotBlank String password) {
}
