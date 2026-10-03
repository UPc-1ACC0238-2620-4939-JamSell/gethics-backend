package com.jamsell.gethics.iam.domain.model.commands;

import java.util.Locale;

public record LoginCommand(String email, String password) {

    public LoginCommand {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
