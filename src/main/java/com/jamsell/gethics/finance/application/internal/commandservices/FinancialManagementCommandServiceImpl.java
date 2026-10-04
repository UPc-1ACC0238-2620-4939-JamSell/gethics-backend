package com.jamsell.gethics.finance.application.internal.commandservices;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.commands.RegisterTransactionCommand;
import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.domain.services.FinancialManagementCommandService;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialManagementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialManagementCommandServiceImpl implements FinancialManagementCommandService {

    private final FinancialManagementRepository financialManagementRepository;

    public FinancialManagementCommandServiceImpl(FinancialManagementRepository financialManagementRepository) {
        this.financialManagementRepository = financialManagementRepository;
    }

    @Override
    @Transactional
    public FinancialTransaction handle(RegisterTransactionCommand command) {
        var financialManagement = financialManagementRepository.findByOwnerId(command.ownerId())
                .orElseGet(() -> new FinancialManagement(command.ownerId()));

        var transaction = financialManagement.register(
                command.type(),
                command.amount(),
                command.category(),
                command.occurredOn(),
                command.description());

        financialManagementRepository.saveAndFlush(financialManagement);
        return transaction;
    }
}
