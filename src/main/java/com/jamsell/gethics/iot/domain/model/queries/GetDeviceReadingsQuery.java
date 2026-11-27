package com.jamsell.gethics.iot.domain.model.queries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * US-37: lecturas de un dispositivo, paginadas y de la mas reciente a la mas antigua. {@code page} empieza en 0 y
 * {@code size} va de 1 a {@link #MAX_SIZE}.
 *
 * <p>US-48: {@code from} y {@code to} son dias de calendario opcionales y ambos inclusivos; null deja ese extremo
 * abierto. Se comparan con {@code recordedAt}, que es un {@link LocalDateTime} sin zona: el dia se toma tal cual.
 */
public record GetDeviceReadingsQuery(UUID deviceId, int page, int size, LocalDate from, LocalDate to) {

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
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("El parametro from no puede ser posterior a to.");
        }
    }

    /** Consulta sin filtro de fechas (US-37). */
    public GetDeviceReadingsQuery(UUID deviceId, int page, int size) {
        this(deviceId, page, size, null, null);
    }

    /** Primer instante incluido ({@code from} a las 00:00), o null si no hay limite inferior. */
    public LocalDateTime fromInclusive() {
        return from == null ? null : from.atStartOfDay();
    }

    /** Primer instante excluido ({@code to + 1} dia a las 00:00), o null si no hay limite superior. */
    public LocalDateTime toExclusive() {
        return to == null ? null : to.plusDays(1).atStartOfDay();
    }
}
