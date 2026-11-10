package com.jamsell.gethics.analytics.application.internal.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LivestockReportCsvWriterTest {

    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String LIMITED = "Los datos aun son limitados para un analisis completo.";

    private final LivestockReportCsvWriter writer = new LivestockReportCsvWriter();

    private static LivestockReport report(FinanceReportSection finance, ReportSection health) {
        return new LivestockReport(OWNER, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), finance, health,
                ReportSection.limited(LIMITED));
    }

    private String csv(LivestockReport report) {
        return new String(writer.write(report), StandardCharsets.UTF_8);
    }

    @Test
    void writesEverySectionOfTheReportAsRowsWithBomAndCrlf() {
        var csv = csv(report(FinanceReportSection.of(new BigDecimal("500.00"), new BigDecimal("120.50")),
                ReportSection.limited(LIMITED)));

        var expected = "﻿"
                + "Seccion,Indicador,Valor\r\n"
                + "Reporte,Propietario,11111111-2222-3333-4444-555555555555\r\n"
                + "Reporte,Desde,2026-03-01\r\n"
                + "Reporte,Hasta,2026-03-31\r\n"
                + "Finanzas,Ingresos totales,500.00\r\n"
                + "Finanzas,Egresos totales,120.50\r\n"
                + "Finanzas,Resultado neto,379.50\r\n"
                + "Finanzas,Datos limitados,No\r\n"
                + "Salud,Datos limitados,Si\r\n"
                + "Salud,Mensaje,Los datos aun son limitados para un analisis completo.\r\n"
                + "Productividad,Datos limitados,Si\r\n"
                + "Productividad,Mensaje,Los datos aun son limitados para un analisis completo.\r\n";
        assertEquals(expected, csv);
    }

    @Test
    void startsWithTheUtf8BomBytesAndKeepsAccents() {
        var bytes = writer.write(report(FinanceReportSection.of(BigDecimal.ONE, BigDecimal.ZERO),
                ReportSection.limited("Información pendiente de revisión")));

        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("Salud,Mensaje,Información pendiente de revisión"));
    }

    @Test
    void quotesCellsWithCommasQuotesOrLineBreaks() {
        var csv = csv(report(FinanceReportSection.of(BigDecimal.ONE, BigDecimal.ZERO),
                ReportSection.limited("Vacunas \"pendientes\", revisar\nmanana")));

        assertTrue(csv.contains("Salud,Mensaje,\"Vacunas \"\"pendientes\"\", revisar\nmanana\"\r\n"));
    }

    @Test
    void writesANegativeNetResultAsAPlainNumber() {
        var csv = csv(report(FinanceReportSection.of(new BigDecimal("100.00"), new BigDecimal("250.00")),
                ReportSection.limited(LIMITED)));

        assertTrue(csv.contains("Finanzas,Resultado neto,-150.00\r\n"));
    }

    @Test
    void escapeLeavesPlainCellsUntouched() {
        assertEquals("Ingresos totales", LivestockReportCsvWriter.escape("Ingresos totales"));
        assertEquals("\"a,b\"", LivestockReportCsvWriter.escape("a,b"));
        assertEquals("", LivestockReportCsvWriter.escape(null));
    }
}
