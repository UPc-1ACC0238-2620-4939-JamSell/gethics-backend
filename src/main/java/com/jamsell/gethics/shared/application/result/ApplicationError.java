package com.jamsell.gethics.shared.application.result;

import java.util.Locale;

public record ApplicationError(String code, String message, String details) {

    public ApplicationError(String code, String message) {
        this(code, message, null);
    }

    public static ApplicationError validationError(String details) {
        return new ApplicationError("VALIDATION_ERROR", "Datos inválidos", details);
    }

    public static ApplicationError notFound(String resource, String message) {
        return new ApplicationError(resource.toUpperCase(Locale.ROOT) + "_NOT_FOUND", message);
    }

    public static ApplicationError conflict(String resource, String message) {
        return new ApplicationError(resource.toUpperCase(Locale.ROOT) + "_CONFLICT", message);
    }

    public static ApplicationError businessRuleViolation(String message) {
        return new ApplicationError("BUSINESS_RULE_VIOLATION", message);
    }

    public static ApplicationError paymentRejected(String message) {
        return new ApplicationError("PAYMENT_REJECTED", message);
    }

    public static ApplicationError invalidCredentials() {
        return new ApplicationError("INVALID_CREDENTIALS", "Correo o contraseña incorrectos");
    }

    public static ApplicationError unauthorized() {
        return new ApplicationError("UNAUTHORIZED", "Autenticación requerida");
    }

    public static ApplicationError forbidden() {
        return new ApplicationError("FORBIDDEN", "No tienes permiso para realizar esta acción");
    }

    public static ApplicationError unexpected() {
        return new ApplicationError("UNEXPECTED_ERROR", "Ocurrió un error inesperado");
    }
}
