package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;

public interface LivestockReportQueryService {

    LivestockReport handle(GetLivestockReportQuery query);
}
