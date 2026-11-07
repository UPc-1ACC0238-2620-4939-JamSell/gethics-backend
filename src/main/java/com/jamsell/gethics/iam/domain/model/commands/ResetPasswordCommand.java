package com.jamsell.gethics.iam.domain.model.commands;

public record ResetPasswordCommand(String token, String newPassword) {

    public ResetPasswordCommand {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token es obligatorio");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña es obligatoria");
        }
        token = token.trim();
    }
}
