package com.jamsell.gethics.finance.domain.model.valueobjects;

import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import java.math.BigDecimal;
import java.util.List;

/**
 * Read model for US-16: income, expense and net balance for a given owner, optionally scoped to a period.
 */
public record FinancialPeriodSummary(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netBalance,
        List<FinancialTransaction> transactions) {

    public boolean hasMovements() {
        return !transactions.isEmpty();
    }
}
