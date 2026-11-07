package com.jamsell.gethics.analytics.application.internal.outboundservices;

import java.time.LocalDate;
import java.util.UUID;

public interface FinancialSummaryLookup {

    FinancialPeriodFigures findSummary(UUID ownerId, LocalDate from, LocalDate to);
}
