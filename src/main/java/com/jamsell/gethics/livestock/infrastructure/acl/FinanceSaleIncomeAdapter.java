package com.jamsell.gethics.livestock.infrastructure.acl;

import com.jamsell.gethics.finance.interfaces.acl.FinanceContextFacade;
import com.jamsell.gethics.livestock.application.internal.outboundservices.SaleIncomeRegistrar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** US-30: el ingreso de la venta se registra en Finance mediante su fachada. */
@Component
public class FinanceSaleIncomeAdapter implements SaleIncomeRegistrar {

    static final String CATEGORY = "Venta de ganado";

    private final FinanceContextFacade finance;

    public FinanceSaleIncomeAdapter(FinanceContextFacade finance) {
        this.finance = finance;
    }

    @Override
    public UUID registerIncome(UUID ownerId, BigDecimal amount, LocalDate saleDate, String description) {
        return finance.registerIncome(ownerId, amount, CATEGORY, saleDate, description);
    }
}
