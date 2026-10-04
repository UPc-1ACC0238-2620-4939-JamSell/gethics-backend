package com.jamsell.gethics.finance.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FinancialSummaryResource(
        UUID ownerId,
        BigDecimal balance,
        List<TransactionResource> transactions) {
}
