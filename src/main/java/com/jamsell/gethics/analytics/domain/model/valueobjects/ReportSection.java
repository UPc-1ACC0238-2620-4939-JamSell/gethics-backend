package com.jamsell.gethics.analytics.domain.model.valueobjects;

/**
 * Seccion de un {@code LivestockReport} para la que hoy no existe una fuente de datos real (salud y productividad):
 * siempre limitada, con el mensaje del Escenario 2 ("datos aun limitados"). Ver {@code LivestockReportQueryServiceImpl}.
 */
public record ReportSection(boolean limitedData, String message) {

    public static ReportSection limited(String message) {
        return new ReportSection(true, message);
    }
}
