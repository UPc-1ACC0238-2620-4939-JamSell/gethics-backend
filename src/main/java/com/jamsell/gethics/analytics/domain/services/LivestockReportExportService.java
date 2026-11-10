package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;

public interface LivestockReportExportService {

    /**
     * US-39: el reporte del periodo como CSV (UTF-8 con BOM). Lanza {@code ReportDataNotAvailableException} si el
     * periodo no tiene informacion, e {@code IllegalArgumentException} si {@code from} es posterior a {@code to}.
     */
    byte[] export(GetLivestockReportQuery query);
}
