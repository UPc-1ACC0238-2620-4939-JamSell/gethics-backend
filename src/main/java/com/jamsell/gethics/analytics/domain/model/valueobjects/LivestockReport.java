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

    /** US-39: true si al menos una seccion tiene datos reales (no es solo el aviso de "datos aun limitados"). */
    public boolean hasAnyData() {
        return !finance.limitedData() || !health.limitedData() || !productivity.limitedData();
    }
}
