package com.jamsell.gethics.subscription.domain.model.commands;

import java.util.Locale;

public record SubscribeToPlanCommand(Long userId, String planCode, String paymentToken) {

    public SubscribeToPlanCommand {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (planCode == null || planCode.isBlank()) {
            throw new IllegalArgumentException("El plan es obligatorio");
        }
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        planCode = planCode.trim().toUpperCase(Locale.ROOT);
        paymentToken = paymentToken.trim();
    }
}
