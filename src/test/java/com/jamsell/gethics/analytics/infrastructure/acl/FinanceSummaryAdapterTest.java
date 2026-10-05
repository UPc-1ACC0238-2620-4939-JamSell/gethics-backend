package com.jamsell.gethics.analytics.infrastructure.acl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.queries.GetTransactionsByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinanceSummaryAdapterTest {

    private final UUID ownerId = UUID.randomUUID();
    private final FinancialManagementQueryService queryService = mock(FinancialManagementQueryService.class);
    private final FinanceSummaryAdapter adapter = new FinanceSummaryAdapter(queryService);

    @Test
    void sumsIncomeAndExpenseOnlyWithinTheRequestedRange() {
        var financialManagement = new FinancialManagement(ownerId);
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("300.00"), "Milk",
                LocalDate.of(2026, 3, 10), null);
        financialManagement.register(FinancialTransactionType.EXPENSE, new BigDecimal("80.00"), "Feed",
                LocalDate.of(2026, 3, 15), null);
        // Fuera del rango solicitado: no debe contarse.
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("1000.00"), "Cattle sale",
                LocalDate.of(2026, 1, 1), null);
        when(queryService.handle(new GetTransactionsByOwnerQuery(ownerId)))
                .thenReturn(financialManagement.getTransactions());

        var figures = adapter.findSummary(ownerId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(0, new BigDecimal("300.00").compareTo(figures.totalIncome()));
        assertEquals(0, new BigDecimal("80.00").compareTo(figures.totalExpense()));
        assertEquals(2, figures.transactionCount());
        assertTrue(figures.hasMovements());
    }

    @Test
    void returnsZeroedFiguresWhenThereAreNoTransactionsInRange() {
        when(queryService.handle(new GetTransactionsByOwnerQuery(ownerId))).thenReturn(List.of());

        var figures = adapter.findSummary(ownerId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(0, BigDecimal.ZERO.compareTo(figures.totalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(figures.totalExpense()));
        assertEquals(0, figures.transactionCount());
        assertFalse(figures.hasMovements());
    }

    @Test
    void includesTheBoundaryDatesOfTheRange() {
        var financialManagement = new FinancialManagement(ownerId);
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("50.00"), "Milk",
                LocalDate.of(2026, 3, 1), null);
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("25.00"), "Milk",
                LocalDate.of(2026, 3, 31), null);
        when(queryService.handle(new GetTransactionsByOwnerQuery(ownerId)))
                .thenReturn(financialManagement.getTransactions());

        var figures = adapter.findSummary(ownerId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(2, figures.transactionCount());
        assertEquals(0, new BigDecimal("75.00").compareTo(figures.totalIncome()));
    }
}
