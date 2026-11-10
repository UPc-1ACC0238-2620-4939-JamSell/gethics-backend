package com.jamsell.gethics.analytics.application.internal.queryservices;

import com.jamsell.gethics.analytics.application.internal.exporters.LivestockReportCsvWriter;
import com.jamsell.gethics.analytics.domain.exceptions.ReportDataNotAvailableException;
import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.services.LivestockReportExportService;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import org.springframework.stereotype.Service;

/**
 * US-39: reutiliza el reporte de US-20 ({@link LivestockReportQueryService}) y lo exporta con
 * {@link LivestockReportCsvWriter}. Si ninguna seccion tiene datos reales, no hay nada que exportar (Escenario 2).
 */
@Service
public class LivestockReportExportServiceImpl implements LivestockReportExportService {

    private final LivestockReportQueryService reportQueryService;
    private final LivestockReportCsvWriter csvWriter;

    public LivestockReportExportServiceImpl(LivestockReportQueryService reportQueryService,
                                            LivestockReportCsvWriter csvWriter) {
        this.reportQueryService = reportQueryService;
        this.csvWriter = csvWriter;
    }

    @Override
    public byte[] export(GetLivestockReportQuery query) {
        var report = reportQueryService.handle(query);
        if (!report.hasAnyData()) {
            throw new ReportDataNotAvailableException();
        }
        return csvWriter.write(report);
    }
}
