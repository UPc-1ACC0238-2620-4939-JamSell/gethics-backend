package com.jamsell.gethics.analytics.domain.model.queries;

import java.time.LocalDate;
import java.util.UUID;

/**
 * US-20: reporte de salud, productividad y finanzas del ganado de un ganadero, para el rango [from, to].
 */
public record GetLivestockReportQuery(UUID ownerId, LocalDate from, LocalDate to) {
}
