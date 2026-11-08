package com.jamsell.gethics.iam.domain.model.commands;

import java.util.Locale;

public record ForgotPasswordCommand(String email) {

    public ForgotPasswordCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
