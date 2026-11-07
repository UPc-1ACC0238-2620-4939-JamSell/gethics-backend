package com.jamsell.gethics.analytics.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

public record LivestockReportResource(
        UUID ownerId,
        LocalDate from,
        LocalDate to,
        FinanceReportSectionResource finance,
        ReportSectionResource health,
        ReportSectionResource productivity) {
}
