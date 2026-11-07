package com.jamsell.gethics.finance.application.internal.queryservices;

import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.domain.model.queries.GetFinancialSummaryByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialPeriodSummary;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialManagementRepository;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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
    public FinancialPeriodSummary handle(GetFinancialSummaryByOwnerQuery query) {
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }

        var transactions = financialManagementRepository.findByOwnerId(query.ownerId())
                .map(fm -> findTransactions(fm.getId(), query))
                .orElseGet(List::of);

        var totalIncome = sumByType(transactions, FinancialTransactionType.INCOME);
        var totalExpense = sumByType(transactions, FinancialTransactionType.EXPENSE);

        return new FinancialPeriodSummary(totalIncome, totalExpense, totalIncome.subtract(totalExpense),
                transactions);
    }

    private List<FinancialTransaction> findTransactions(UUID financialManagementId,
                                                         GetFinancialSummaryByOwnerQuery query) {
        return query.from() == null || query.to() == null
                ? financialTransactionRepository.findByFinancialManagementIdOrderByOccurredOnDesc(
                        financialManagementId)
                : financialTransactionRepository.findByFinancialManagementIdAndOccurredOnBetweenOrderByOccurredOnDesc(
                        financialManagementId, query.from(), query.to());
    }

    private static BigDecimal sumByType(List<FinancialTransaction> transactions, FinancialTransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
