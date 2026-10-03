package com.jamsell.gethics.finance.domain.model.commands;

import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterTransactionCommand(
        UUID ownerId,
        FinancialTransactionType type,
        BigDecimal amount,
        String category,
        LocalDate occurredOn,
        String description) {
}
