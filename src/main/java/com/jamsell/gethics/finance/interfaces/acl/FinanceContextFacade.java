package com.jamsell.gethics.finance.interfaces.acl;

import com.jamsell.gethics.finance.domain.model.commands.RegisterTransactionCommand;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.domain.services.FinancialManagementCommandService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fachada de Finance para otros bounded contexts (hoy, Livestock: ingreso por la venta de un animal, US-30). */
@Service
public class FinanceContextFacade {

    private final FinancialManagementCommandService commandService;

    public FinanceContextFacade(FinancialManagementCommandService commandService) {
        this.commandService = commandService;
    }

    /** Registra un ingreso del ganadero y devuelve el id del movimiento creado. */
    public UUID registerIncome(UUID ownerId, BigDecimal amount, String category, LocalDate date, String description) {
        var transaction = commandService.handle(new RegisterTransactionCommand(ownerId,
                FinancialTransactionType.INCOME, amount, category, date, description));
        return transaction.getId();
    }
}
