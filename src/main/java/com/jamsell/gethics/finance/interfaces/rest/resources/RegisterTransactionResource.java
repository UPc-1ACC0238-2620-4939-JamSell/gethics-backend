package com.jamsell.gethics.finance.interfaces.rest.resources;

import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterTransactionResource(
        @NotNull(message = "Owner id is required") UUID ownerId,
        @NotNull(message = "Type is required") FinancialTransactionType type,
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero") BigDecimal amount,
        @NotBlank(message = "Category is required") String category,
        @NotNull(message = "Date is required") LocalDate date,
        String description) {
}
