package com.jamsell.gethics.finance.interfaces.rest.transform;

import com.jamsell.gethics.finance.domain.model.commands.RegisterTransactionCommand;
import com.jamsell.gethics.finance.interfaces.rest.resources.RegisterTransactionResource;

public final class RegisterTransactionCommandFromResourceAssembler {

    private RegisterTransactionCommandFromResourceAssembler() {
    }

    public static RegisterTransactionCommand toCommandFromResource(RegisterTransactionResource resource) {
        return new RegisterTransactionCommand(
                resource.ownerId(),
                resource.type(),
                resource.amount(),
                resource.category(),
                resource.date(),
                resource.description());
    }
}
