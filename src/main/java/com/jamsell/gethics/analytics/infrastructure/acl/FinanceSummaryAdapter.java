package com.jamsell.gethics.analytics.infrastructure.acl;

import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialPeriodFigures;
import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialSummaryLookup;
import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter real (no temporal): delega en la consulta de balance por periodo que expone finance (US-16), que ya
 * filtra por el rango [from, to] (ambos inclusive) y totaliza ingresos y egresos. Esta clase solo traduce ese
 * resultado al modelo de analytics.
 */
@Component
public class FinanceSummaryAdapter implements FinancialSummaryLookup {

    private final FinancialManagementQueryService financialManagementQueryService;

    public FinanceSummaryAdapter(FinancialManagementQueryService financialManagementQueryService) {
        this.financialManagementQueryService = financialManagementQueryService;
    }

    @Override
    public FinancialPeriodFigures findSummary(UUID ownerId, LocalDate from, LocalDate to) {
        var summary = financialManagementQueryService.handle(new GetFinancialSummaryByOwnerQuery(ownerId, from, to));
        return new FinancialPeriodFigures(summary.totalIncome(), summary.totalExpense(),
                summary.transactions().size());
    }
}
