package com.jamsell.gethics.finance.application.internal.queryservices;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.domain.model.queries.GetBalanceByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.queries.GetTransactionsByOwnerQuery;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialManagementRepository;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialManagementQueryServiceImpl implements FinancialManagementQueryService {

    private final FinancialManagementRepository financialManagementRepository;
    private final FinancialTransactionRepository financialTransactionRepository;

    public FinancialManagementQueryServiceImpl(FinancialManagementRepository financialManagementRepository,
                                               FinancialTransactionRepository financialTransactionRepository) {
        this.financialManagementRepository = financialManagementRepository;
        this.financialTransactionRepository = financialTransactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialTransaction> handle(GetTransactionsByOwnerQuery query) {
        return financialManagementRepository.findByOwnerId(query.ownerId())
                .map(fm -> financialTransactionRepository
                        .findByFinancialManagementIdOrderByOccurredOnDesc(fm.getId()))
                .orElseGet(List::of);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal handle(GetBalanceByOwnerQuery query) {
        return financialManagementRepository.findByOwnerId(query.ownerId())
                .map(FinancialManagement::getBalance)
                .orElse(BigDecimal.ZERO);
    }
}
