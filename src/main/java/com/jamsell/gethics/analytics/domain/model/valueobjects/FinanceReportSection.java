package com.jamsell.gethics.analytics.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Seccion financiera de un {@code LivestockReport}: ingresos, egresos y resultado neto del periodo solicitado,
 * calculados a partir de las transacciones reales del contexto finance. {@code limitedData} es true (Escenario 2)
 * cuando no hay ninguna transaccion registrada en el periodo.
 */
public record FinanceReportSection(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netResult,
        boolean limitedData,
        String message) {

    public static FinanceReportSection limited(String message) {
        return new FinanceReportSection(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true, message);
    }

    public static FinanceReportSection of(BigDecimal totalIncome, BigDecimal totalExpense) {
        return new FinanceReportSection(totalIncome, totalExpense, totalIncome.subtract(totalExpense), false, null);
    }
}
