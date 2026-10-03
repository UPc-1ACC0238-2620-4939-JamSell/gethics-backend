package com.jamsell.gethics.finance.domain.services;

import com.jamsell.gethics.finance.domain.model.commands.RegisterTransactionCommand;
import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;

public interface FinancialManagementCommandService {

    FinancialTransaction handle(RegisterTransactionCommand command);
}
