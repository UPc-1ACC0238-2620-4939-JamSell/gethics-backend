package com.jamsell.gethics.iam.domain.model.commands;

import com.jamsell.gethics.iam.domain.model.valueobjects.Role;

import java.util.Locale;

public record RegisterUserCommand(String name, String email, String password, Role role) {

    public RegisterUserCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        if (role == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
        name = name.trim();
        email = email.trim().toLowerCase(Locale.ROOT);
    }
}
