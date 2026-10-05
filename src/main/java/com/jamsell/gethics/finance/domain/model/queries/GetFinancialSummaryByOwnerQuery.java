package com.jamsell.gethics.finance.domain.model.queries;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Both dates null returns the all-time summary; both must be given together to filter by period.
 */
public record GetFinancialSummaryByOwnerQuery(UUID ownerId, LocalDate from, LocalDate to) {
}
