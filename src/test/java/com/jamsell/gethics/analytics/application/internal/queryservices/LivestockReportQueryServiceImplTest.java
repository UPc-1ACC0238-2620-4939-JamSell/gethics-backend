package com.jamsell.gethics.analytics.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialPeriodFigures;
import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialSummaryLookup;
import com.jamsell.gethics.analytics.application.internal.outboundservices.HealthInsightsLookup;
import com.jamsell.gethics.analytics.application.internal.outboundservices.ProductivityInsightsLookup;
import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LivestockReportQueryServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    private final FinancialSummaryLookup financialSummaryLookup = mock(FinancialSummaryLookup.class);
    private final HealthInsightsLookup healthInsightsLookup = mock(HealthInsightsLookup.class);
    private final ProductivityInsightsLookup productivityInsightsLookup = mock(ProductivityInsightsLookup.class);
    private final LivestockReportQueryServiceImpl service = new LivestockReportQueryServiceImpl(
            financialSummaryLookup, healthInsightsLookup, productivityInsightsLookup);

    // --- Escenario 1: reportes por rango de fechas (finanzas con datos reales) ---

    @Test
    void financeSectionUsesRealFiguresWhenThereAreMovements() {
        when(financialSummaryLookup.findSummary(OWNER_ID, FROM, TO))
                .thenReturn(new FinancialPeriodFigures(new BigDecimal("500.00"), new BigDecimal("120.00"), 3));

        var report = service.handle(new GetLivestockReportQuery(OWNER_ID, FROM, TO));

        assertEquals(OWNER_ID, report.ownerId());
        assertEquals(FROM, report.from());
        assertEquals(TO, report.to());
        assertFalse(report.finance().limitedData());
        assertEquals(0, new BigDecimal("500.00").compareTo(report.finance().totalIncome()));
        assertEquals(0, new BigDecimal("120.00").compareTo(report.finance().totalExpense()));
        assertEquals(0, new BigDecimal("380.00").compareTo(report.finance().netResult()));
    }

    // --- Escenario 2: datos historicos limitados ---

    @Test
    void financeSectionIsLimitedWhenThereAreNoMovementsInTheRange() {
        when(financialSummaryLookup.findSummary(OWNER_ID, FROM, TO))
                .thenReturn(new FinancialPeriodFigures(BigDecimal.ZERO, BigDecimal.ZERO, 0));

        var report = service.handle(new GetLivestockReportQuery(OWNER_ID, FROM, TO));

        assertTrue(report.finance().limitedData());
        assertEquals(LivestockReportQueryServiceImpl.LIMITED_DATA_MESSAGE, report.finance().message());
    }

    @Test
    void healthAndProductivitySectionsAreAlwaysLimitedToday() {
        when(financialSummaryLookup.findSummary(any(), any(), any()))
                .thenReturn(new FinancialPeriodFigures(BigDecimal.ZERO, BigDecimal.ZERO, 0));
        when(healthInsightsLookup.hasSufficientData(OWNER_ID, FROM, TO)).thenReturn(false);
        when(productivityInsightsLookup.hasSufficientData(OWNER_ID, FROM, TO)).thenReturn(false);

        var report = service.handle(new GetLivestockReportQuery(OWNER_ID, FROM, TO));

        assertTrue(report.health().limitedData());
        assertEquals(LivestockReportQueryServiceImpl.LIMITED_DATA_MESSAGE, report.health().message());
        assertTrue(report.productivity().limitedData());
        assertEquals(LivestockReportQueryServiceImpl.LIMITED_DATA_MESSAGE, report.productivity().message());
    }

    @Test
    void healthSectionIsNotLimitedWhenTheLookupReportsEnoughData() {
        when(financialSummaryLookup.findSummary(any(), any(), any()))
                .thenReturn(new FinancialPeriodFigures(BigDecimal.ZERO, BigDecimal.ZERO, 0));
        when(healthInsightsLookup.hasSufficientData(OWNER_ID, FROM, TO)).thenReturn(true);
        when(productivityInsightsLookup.hasSufficientData(OWNER_ID, FROM, TO)).thenReturn(false);

        var report = service.handle(new GetLivestockReportQuery(OWNER_ID, FROM, TO));

        assertFalse(report.health().limitedData());
        assertTrue(report.productivity().limitedData());
    }

    @Test
    void fromAfterToThrowsWithoutCallingAnyLookup() {
        var query = new GetLivestockReportQuery(OWNER_ID, TO, FROM);

        assertThrows(IllegalArgumentException.class, () -> service.handle(query));

        verifyNoInteractions(financialSummaryLookup, healthInsightsLookup, productivityInsightsLookup);
    }
}
