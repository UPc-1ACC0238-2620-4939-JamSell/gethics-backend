package com.jamsell.gethics.analytics.application.internal.queryservices;

import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialSummaryLookup;
import com.jamsell.gethics.analytics.application.internal.outboundservices.HealthInsightsLookup;
import com.jamsell.gethics.analytics.application.internal.outboundservices.ProductivityInsightsLookup;
import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import org.springframework.stereotype.Service;

/**
 * US-20: finanzas se calcula con datos reales (via {@link FinancialSummaryLookup}, que llama al contexto finance).
 * Salud y productividad siempre devuelven el aviso del Escenario 2 ("datos aun limitados"), porque hoy no existe
 * ninguna fuente de datos real para ellas (ver {@link HealthInsightsLookup} y {@link ProductivityInsightsLookup}, y
 * sus adapters temporales en infrastructure/acl). Esto no es una limitacion de este query service: es el estado
 * real del proyecto hoy, documentado aqui tal como lo exige el Escenario 2.
 */
@Service
public class LivestockReportQueryServiceImpl implements LivestockReportQueryService {

    static final String LIMITED_DATA_MESSAGE = "Los datos aun son limitados para un analisis completo.";

    private final FinancialSummaryLookup financialSummaryLookup;
    private final HealthInsightsLookup healthInsightsLookup;
    private final ProductivityInsightsLookup productivityInsightsLookup;

    public LivestockReportQueryServiceImpl(FinancialSummaryLookup financialSummaryLookup,
                                           HealthInsightsLookup healthInsightsLookup,
                                           ProductivityInsightsLookup productivityInsightsLookup) {
        this.financialSummaryLookup = financialSummaryLookup;
        this.healthInsightsLookup = healthInsightsLookup;
        this.productivityInsightsLookup = productivityInsightsLookup;
    }

    @Override
    public LivestockReport handle(GetLivestockReportQuery query) {
        if (query.from().isAfter(query.to())) {
            throw new IllegalArgumentException("'from' no puede ser posterior a 'to'.");
        }

        return new LivestockReport(
                query.ownerId(),
                query.from(),
                query.to(),
                financeSection(query),
                healthSection(query),
                productivitySection(query));
    }

    private FinanceReportSection financeSection(GetLivestockReportQuery query) {
        var figures = financialSummaryLookup.findSummary(query.ownerId(), query.from(), query.to());
        return figures.hasMovements()
                ? FinanceReportSection.of(figures.totalIncome(), figures.totalExpense())
                : FinanceReportSection.limited(LIMITED_DATA_MESSAGE);
    }

    private ReportSection healthSection(GetLivestockReportQuery query) {
        return healthInsightsLookup.hasSufficientData(query.ownerId(), query.from(), query.to())
                ? new ReportSection(false, null)
                : ReportSection.limited(LIMITED_DATA_MESSAGE);
    }

    private ReportSection productivitySection(GetLivestockReportQuery query) {
        return productivityInsightsLookup.hasSufficientData(query.ownerId(), query.from(), query.to())
                ? new ReportSection(false, null)
                : ReportSection.limited(LIMITED_DATA_MESSAGE);
    }
}
