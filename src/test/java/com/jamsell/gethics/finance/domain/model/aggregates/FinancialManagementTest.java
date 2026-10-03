package com.jamsell.gethics.finance.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinancialManagementTest {

    private final FinancialManagement financialManagement = new FinancialManagement(UUID.randomUUID());

    @Test
    void registerIncomeIncreasesBalance() {
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("150.00"),
                "Milk sale", LocalDate.now(), null);

        assertEquals(0, new BigDecimal("150.00").compareTo(financialManagement.getBalance()));
        assertEquals(1, financialManagement.getTransactions().size());
    }

    @Test
    void registerExpenseDecreasesBalance() {
        financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("200.00"),
                "Cattle sale", LocalDate.now(), null);
        financialManagement.register(FinancialTransactionType.EXPENSE, new BigDecimal("50.50"),
                "Feed", LocalDate.now(), null);

        assertEquals(0, new BigDecimal("149.50").compareTo(financialManagement.getBalance()));
    }

    @Test
    void registerZeroAmountThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                financialManagement.register(FinancialTransactionType.INCOME, BigDecimal.ZERO,
                        "Invalid", LocalDate.now(), null));
    }

    @Test
    void registerNegativeAmountThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                financialManagement.register(FinancialTransactionType.EXPENSE, new BigDecimal("-10"),
                        "Invalid", LocalDate.now(), null));
    }

    @Test
    void invalidRegistrationDoesNotChangeBalance() {
        assertThrows(IllegalArgumentException.class, () ->
                financialManagement.register(FinancialTransactionType.INCOME, BigDecimal.ZERO,
                        "Invalid", LocalDate.now(), null));

        assertEquals(0, BigDecimal.ZERO.compareTo(financialManagement.getBalance()));
        assertEquals(0, financialManagement.getTransactions().size());
    }
}
