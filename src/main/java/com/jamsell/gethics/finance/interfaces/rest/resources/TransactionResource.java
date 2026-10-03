package com.jamsell.gethics.finance.interfaces.rest.resources;

import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResource(
        UUID id,
        FinancialTransactionType type,
        BigDecimal amount,
        String category,
        LocalDate date,
        String description) {
}
