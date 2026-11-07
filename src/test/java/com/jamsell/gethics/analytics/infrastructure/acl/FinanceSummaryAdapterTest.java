package com.jamsell.gethics.analytics.infrastructure.acl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialPeriodSummary;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinanceSummaryAdapterTest {

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    private final UUID ownerId = UUID.randomUUID();
    private final FinancialManagementQueryService queryService = mock(FinancialManagementQueryService.class);
    private final FinanceSummaryAdapter adapter = new FinanceSummaryAdapter(queryService);

    @Test
    void asksFinanceForThePeriodAndMapsItsTotals() {
        var financialManagement = new FinancialManagement(ownerId);
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("300.00"), "Milk",
                LocalDate.of(2026, 3, 10), null);
        financialManagement.register(FinancialTransactionType.EXPENSE, new BigDecimal("80.00"), "Feed",
                LocalDate.of(2026, 3, 15), null);
        var query = new GetFinancialSummaryByOwnerQuery(ownerId, FROM, TO);
        when(queryService.handle(query)).thenReturn(new FinancialPeriodSummary(new BigDecimal("300.00"),
                new BigDecimal("80.00"), new BigDecimal("220.00"), financialManagement.getTransactions()));

        var figures = adapter.findSummary(ownerId, FROM, TO);

        verify(queryService).handle(query);
        assertEquals(0, new BigDecimal("300.00").compareTo(figures.totalIncome()));
        assertEquals(0, new BigDecimal("80.00").compareTo(figures.totalExpense()));
        assertEquals(2, figures.transactionCount());
        assertTrue(figures.hasMovements());
    }

    @Test
    void returnsZeroedFiguresWhenFinanceHasNoMovementsInThePeriod() {
        when(queryService.handle(new GetFinancialSummaryByOwnerQuery(ownerId, FROM, TO)))
                .thenReturn(new FinancialPeriodSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of()));

        var figures = adapter.findSummary(ownerId, FROM, TO);

        assertEquals(0, BigDecimal.ZERO.compareTo(figures.totalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(figures.totalExpense()));
        assertEquals(0, figures.transactionCount());
        assertFalse(figures.hasMovements());
    }
}
