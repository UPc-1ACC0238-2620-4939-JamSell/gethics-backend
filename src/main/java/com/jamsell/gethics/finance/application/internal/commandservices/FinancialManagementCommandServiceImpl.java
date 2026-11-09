package com.jamsell.gethics.finance.application.internal.commandservices;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.commands.RegisterTransactionCommand;
import com.jamsell.gethics.finance.domain.model.commands.VoidTransactionCommand;
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
    public void handle(VoidTransactionCommand command) {
        // 1. Buscamos el agregado que contiene la transacción (o puedes buscar en todas las finanzas)
        // Como las transacciones pertenecen a FinancialManagement, buscamos todos o iteramos,
        // o si prefieres un enfoque más directo, puedes añadir un método en el repositorio
        // para buscar el FinancialManagement que contenga la transacción por su ID.

        var allManagements = financialManagementRepository.findAll();

        FinancialManagement targetManagement = null;
        FinancialTransaction targetTransaction = null;

        for (FinancialManagement management : allManagements) {
            var transactionOpt = management.getTransactions().stream()
                    .filter(t -> t.getId().equals(command.transactionId()))
                    .findFirst();
            if (transactionOpt.isPresent()) {
                targetManagement = management;
                targetTransaction = transactionOpt.get();
                break;
            }
        }

        if (targetTransaction == null || targetManagement == null) {
            throw new IllegalArgumentException("Financial transaction not found with id: " + command.transactionId());
        }

        // 2. Validar si ya está anulada
        if (targetTransaction.isVoided()) {
            throw new IllegalArgumentException("The financial transaction is already voided.");
        }

        // 3. Anular la transacción dentro del agregado (o llamando al método correspondiente)
        targetTransaction.voidTransaction(); // Asegúrate de tener este método en tu entidad FinancialTransaction

        // 4. Guardar los cambios
        financialManagementRepository.saveAndFlush(targetManagement);
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
