package com.jamsell.gethics.analytics.application.internal.exporters;

import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * US-39: convierte un {@link LivestockReport} en CSV (RFC 4180: separador coma, fin de linea CRLF). Cada fila es
 * {@code Seccion,Indicador,Valor}, asi las tres secciones del reporte caben en una sola tabla. Empieza con BOM UTF-8
 * para que Excel muestre bien los acentos.
 */
@Component
public class LivestockReportCsvWriter {

    static final String BOM = "﻿";
    static final String LINE_END = "\r\n";

    public byte[] write(LivestockReport report) {
        var csv = new StringBuilder(BOM);
        row(csv, "Seccion", "Indicador", "Valor");
        row(csv, "Reporte", "Propietario", report.ownerId().toString());
        row(csv, "Reporte", "Desde", report.from().toString());
        row(csv, "Reporte", "Hasta", report.to().toString());
        finance(csv, report.finance());
        section(csv, "Salud", report.health());
        section(csv, "Productividad", report.productivity());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void finance(StringBuilder csv, FinanceReportSection finance) {
        row(csv, "Finanzas", "Ingresos totales", amount(finance.totalIncome()));
        row(csv, "Finanzas", "Egresos totales", amount(finance.totalExpense()));
        row(csv, "Finanzas", "Resultado neto", amount(finance.netResult()));
        limitedData(csv, "Finanzas", finance.limitedData(), finance.message());
    }

    private static void section(StringBuilder csv, String name, ReportSection section) {
        limitedData(csv, name, section.limitedData(), section.message());
    }

    private static void limitedData(StringBuilder csv, String section, boolean limited, String message) {
        row(csv, section, "Datos limitados", limited ? "Si" : "No");
        if (message != null) {
            row(csv, section, "Mensaje", message);
        }
    }

    private static String amount(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static void row(StringBuilder csv, String... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(cells[i]));
        }
        csv.append(LINE_END);
    }

    /** Entre comillas si la celda tiene coma, comillas o salto de linea; las comillas internas se duplican. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
