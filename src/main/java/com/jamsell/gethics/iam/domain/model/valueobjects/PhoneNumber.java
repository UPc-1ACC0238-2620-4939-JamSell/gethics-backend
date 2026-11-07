package com.jamsell.gethics.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

public final class PhoneNumber {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-().]");
    private static final Pattern VALID_FORMAT = Pattern.compile("^\\+?[0-9]{7,15}$");

    private PhoneNumber() {
    }

    public static String normalizeOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        var normalized = SEPARATORS.matcher(raw.trim()).replaceAll("");
        if (!VALID_FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("El formato del teléfono es inválido");
        }
        return normalized;
    }
}
