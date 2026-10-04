package com.jamsell.gethics.finance.interfaces.rest.transform;

import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import com.jamsell.gethics.finance.interfaces.rest.resources.TransactionResource;

public final class TransactionResourceFromEntityAssembler {

    private TransactionResourceFromEntityAssembler() {
    }

    public static TransactionResource toResourceFromEntity(FinancialTransaction entity) {
        return new TransactionResource(
                entity.getId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCategory(),
                entity.getOccurredOn(),
                entity.getDescription());
    }
}
