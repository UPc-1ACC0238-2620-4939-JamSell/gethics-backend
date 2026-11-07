package com.jamsell.gethics.analytics.interfaces.rest.resources;

import java.math.BigDecimal;

public record FinanceReportSectionResource(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netResult,
        boolean limitedData,
        String message) {
}
