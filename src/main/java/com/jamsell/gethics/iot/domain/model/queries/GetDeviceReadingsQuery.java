package com.jamsell.gethics.iot.domain.model.queries;

import java.util.UUID;

/**
 * US-37: lecturas de un dispositivo, paginadas y de la mas reciente a la mas antigua. {@code page} empieza en 0 y
 * {@code size} va de 1 a {@link #MAX_SIZE}.
 */
public record GetDeviceReadingsQuery(UUID deviceId, int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public GetDeviceReadingsQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("El id del dispositivo es obligatorio.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("El parametro page no puede ser negativo.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El parametro size debe estar entre 1 y " + MAX_SIZE + ".");
        }
    }
}
