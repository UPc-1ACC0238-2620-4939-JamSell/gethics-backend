package com.jamsell.gethics.finance.domain.services;

import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialPeriodSummary;

public interface FinancialManagementQueryService {

    FinancialPeriodSummary handle(GetFinancialSummaryByOwnerQuery query);
}
