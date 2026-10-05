package com.jamsell.gethics.analytics.application.internal.outboundservices;

import java.math.BigDecimal;

/**
 * Cifras reales del contexto finance para un periodo, segun las necesita la seccion financiera de US-20.
 */
public record FinancialPeriodFigures(BigDecimal totalIncome, BigDecimal totalExpense, int transactionCount) {

    public boolean hasMovements() {
        return transactionCount > 0;
    }
}
