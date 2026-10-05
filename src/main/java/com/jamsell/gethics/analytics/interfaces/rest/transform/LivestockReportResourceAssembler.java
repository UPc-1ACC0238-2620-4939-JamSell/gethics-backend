package com.jamsell.gethics.analytics.interfaces.rest.transform;

import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import com.jamsell.gethics.analytics.interfaces.rest.resources.FinanceReportSectionResource;
import com.jamsell.gethics.analytics.interfaces.rest.resources.LivestockReportResource;
import com.jamsell.gethics.analytics.interfaces.rest.resources.ReportSectionResource;

public final class LivestockReportResourceAssembler {

    private LivestockReportResourceAssembler() {
    }

    public static LivestockReportResource toResourceFromEntity(LivestockReport report) {
        return new LivestockReportResource(
                report.ownerId(),
                report.from(),
                report.to(),
                toResourceFromEntity(report.finance()),
                toResourceFromEntity(report.health()),
                toResourceFromEntity(report.productivity()));
    }

    private static FinanceReportSectionResource toResourceFromEntity(FinanceReportSection section) {
        return new FinanceReportSectionResource(
                section.totalIncome(),
                section.totalExpense(),
                section.netResult(),
                section.limitedData(),
                section.message());
    }

    private static ReportSectionResource toResourceFromEntity(ReportSection section) {
        return new ReportSectionResource(section.limitedData(), section.message());
    }
}
