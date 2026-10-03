package com.jamsell.gethics.subscription.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record SubscribeResource(@NotBlank String planCode, @NotBlank String paymentToken) {
}
