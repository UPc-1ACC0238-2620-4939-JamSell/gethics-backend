package com.jamsell.gethics.analytics.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model de US-20 (reportes y estadisticas del ganado) para el rango [from, to] de un ganadero.
 */
public record LivestockReport(
        UUID ownerId,
        LocalDate from,
        LocalDate to,
        FinanceReportSection finance,
        ReportSection health,
        ReportSection productivity) {
}
