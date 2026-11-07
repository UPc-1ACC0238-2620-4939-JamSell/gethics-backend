package com.jamsell.gethics.finance.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialManagementRepository;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialManagementQueryServiceImplTest {

    private final FinancialManagementRepository financialManagementRepository =
            mock(FinancialManagementRepository.class);
    private final FinancialTransactionRepository financialTransactionRepository =
            mock(FinancialTransactionRepository.class);
    private final FinancialManagementQueryServiceImpl service = new FinancialManagementQueryServiceImpl(
            financialManagementRepository, financialTransactionRepository);

    private final UUID ownerId = UUID.randomUUID();

    @Test
    void returnsZeroSummaryWithMessageWhenOwnerHasNoFinancialManagement() {
        when(financialManagementRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        var summary = service.handle(new GetFinancialSummaryByOwnerQuery(ownerId, null, null));

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalIncome()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalExpense()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.netBalance()));
        assertFalse(summary.hasMovements());
    }

    @Test
    void returnsAllTimeTotalsWhenNoPeriodIsGiven() {
        var financialManagement = new FinancialManagement(ownerId);
        when(financialManagementRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(financialManagement));
        var income = financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("200.00"),
                "Cattle sale", LocalDate.of(2026, 9, 1), null);
        var expense = financialManagement.register(FinancialTransactionType.EXPENSE, new BigDecimal("50.00"),
                "Feed", LocalDate.of(2026, 9, 10), null);
        when(financialTransactionRepository.findByFinancialManagementIdOrderByOccurredOnDesc(
                financialManagement.getId())).thenReturn(List.of(expense, income));

        var summary = service.handle(new GetFinancialSummaryByOwnerQuery(ownerId, null, null));

        assertEquals(0, new BigDecimal("200.00").compareTo(summary.totalIncome()));
        assertEquals(0, new BigDecimal("50.00").compareTo(summary.totalExpense()));
        assertEquals(0, new BigDecimal("150.00").compareTo(summary.netBalance()));
        assertTrue(summary.hasMovements());
    }

    @Test
    void filtersTransactionsByPeriodWhenFromAndToAreGiven() {
        var financialManagement = new FinancialManagement(ownerId);
        when(financialManagementRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(financialManagement));
        var from = LocalDate.of(2026, 9, 1);
        var to = LocalDate.of(2026, 9, 30);
        var income = financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("100.00"),
                "Milk sale", LocalDate.of(2026, 9, 15), null);
        when(financialTransactionRepository.findByFinancialManagementIdAndOccurredOnBetweenOrderByOccurredOnDesc(
                financialManagement.getId(), from, to)).thenReturn(List.of(income));

        var summary = service.handle(new GetFinancialSummaryByOwnerQuery(ownerId, from, to));

        assertEquals(0, new BigDecimal("100.00").compareTo(summary.totalIncome()));
        assertEquals(1, summary.transactions().size());
    }

    @Test
    void periodWithoutMovementsReturnsZeroBalanceAndNoMovements() {
        var financialManagement = new FinancialManagement(ownerId);
        when(financialManagementRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(financialManagement));
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 1, 31);
        when(financialTransactionRepository.findByFinancialManagementIdAndOccurredOnBetweenOrderByOccurredOnDesc(
                financialManagement.getId(), from, to)).thenReturn(List.of());

        var summary = service.handle(new GetFinancialSummaryByOwnerQuery(ownerId, from, to));

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.netBalance()));
        assertFalse(summary.hasMovements());
    }

    @Test
    void throwsWhenFromIsAfterTo() {
        var from = LocalDate.of(2026, 9, 30);
        var to = LocalDate.of(2026, 9, 1);

        assertThrows(IllegalArgumentException.class,
                () -> service.handle(new GetFinancialSummaryByOwnerQuery(ownerId, from, to)));
    }
}
