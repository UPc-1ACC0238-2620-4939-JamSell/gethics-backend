package com.jamsell.gethics.iam.domain.model.valueobjects;

public record ProfileImage(byte[] bytes, String contentType) {

    public static final int MAX_SIZE_BYTES = 2 * 1024 * 1024;

    public static ProfileImage from(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }
        if (bytes.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido de 2 MB");
        }
        return new ProfileImage(bytes, detectContentType(bytes));
    }

    private static String detectContentType(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (bytes.length > 12 && startsWith(bytes, 'R', 'I', 'F', 'F') && matchesAt(bytes, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        throw new IllegalArgumentException("Formato de imagen no soportado. Usa JPG, PNG o WebP");
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        return matchesAt(bytes, 0, signature);
    }

    private static boolean matchesAt(byte[] bytes, int offset, int... signature) {
        if (bytes.length < offset + signature.length) {
            return false;
        }
        for (var i = 0; i < signature.length; i++) {
            if ((bytes[offset + i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
