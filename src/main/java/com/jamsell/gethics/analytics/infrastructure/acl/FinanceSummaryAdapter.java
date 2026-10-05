package com.jamsell.gethics.analytics.infrastructure.acl;

import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialPeriodFigures;
import com.jamsell.gethics.analytics.application.internal.outboundservices.FinancialSummaryLookup;
import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.domain.model.queries.GetTransactionsByOwnerQuery;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.domain.services.FinancialManagementQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter real (no temporal): el contexto finance ya expone {@link FinancialManagementQueryService}, asi que esta
 * clase lo llama directamente y recorta el resultado al rango [from, to], porque esa consulta todavia no filtra
 * por periodo (a diferencia de livestock y analytics, finance no necesita un stub aqui).
 */
@Component
public class FinanceSummaryAdapter implements FinancialSummaryLookup {

    private final FinancialManagementQueryService financialManagementQueryService;

    public FinanceSummaryAdapter(FinancialManagementQueryService financialManagementQueryService) {
        this.financialManagementQueryService = financialManagementQueryService;
    }

    @Override
    public FinancialPeriodFigures findSummary(UUID ownerId, LocalDate from, LocalDate to) {
        List<FinancialTransaction> transactionsInRange = financialManagementQueryService
                .handle(new GetTransactionsByOwnerQuery(ownerId)).stream()
                .filter(transaction -> isWithinRange(transaction.getOccurredOn(), from, to))
                .toList();

        return new FinancialPeriodFigures(
                sumByType(transactionsInRange, FinancialTransactionType.INCOME),
                sumByType(transactionsInRange, FinancialTransactionType.EXPENSE),
                transactionsInRange.size());
    }

    private static boolean isWithinRange(LocalDate occurredOn, LocalDate from, LocalDate to) {
        return !occurredOn.isBefore(from) && !occurredOn.isAfter(to);
    }

    private static BigDecimal sumByType(List<FinancialTransaction> transactions, FinancialTransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
