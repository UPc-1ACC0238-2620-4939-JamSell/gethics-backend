package com.jamsell.gethics.finance.domain.services;

import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.domain.model.queries.GetBalanceByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.queries.GetTransactionsByOwnerQuery;
import java.math.BigDecimal;
import java.util.List;

public interface FinancialManagementQueryService {

    List<FinancialTransaction> handle(GetTransactionsByOwnerQuery query);

    BigDecimal handle(GetBalanceByOwnerQuery query);
}
