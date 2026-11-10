package com.jamsell.gethics.analytics.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.analytics.application.internal.exporters.LivestockReportCsvWriter;
import com.jamsell.gethics.analytics.domain.exceptions.ReportDataNotAvailableException;
import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LivestockReportExportServiceImplTest {

    private static final String LIMITED = "Los datos aun son limitados para un analisis completo.";

    private final LivestockReportQueryService reportQueryService = mock(LivestockReportQueryService.class);
    private final LivestockReportCsvWriter csvWriter = mock(LivestockReportCsvWriter.class);
    private final LivestockReportExportServiceImpl service =
            new LivestockReportExportServiceImpl(reportQueryService, csvWriter);

    private final GetLivestockReportQuery query =
            new GetLivestockReportQuery(UUID.randomUUID(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    private LivestockReport report(FinanceReportSection finance) {
        return new LivestockReport(query.ownerId(), query.from(), query.to(), finance, ReportSection.limited(LIMITED),
                ReportSection.limited(LIMITED));
    }

    @Test
    void exportsTheReportOfThePeriodWhenItHasData() {
        var report = report(FinanceReportSection.of(new BigDecimal("300.00"), new BigDecimal("80.00")));
        var csv = new byte[] {1, 2, 3};
        when(reportQueryService.handle(query)).thenReturn(report);
        when(csvWriter.write(report)).thenReturn(csv);

        assertArrayEquals(csv, service.export(query));
    }

    @Test
    void throwsNoDataWhenEverySectionOfThePeriodIsLimited() {
        when(reportQueryService.handle(query)).thenReturn(report(FinanceReportSection.limited(LIMITED)));

        var exception = assertThrows(ReportDataNotAvailableException.class, () -> service.export(query));

        assertEquals("No hay información disponible para el periodo seleccionado", exception.getMessage());
        verify(csvWriter, never()).write(any());
    }

    @Test
    void propagatesTheInvalidRangeErrorOfTheReportQuery() {
        when(reportQueryService.handle(query)).thenThrow(new IllegalArgumentException("'from' no puede ser posterior a 'to'."));

        assertThrows(IllegalArgumentException.class, () -> service.export(query));
        verify(csvWriter, never()).write(any());
    }
}
