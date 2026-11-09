package com.jamsell.gethics.finance.domain.model.commands;

import java.util.UUID;

public record VoidTransactionCommand(UUID transactionId) {
    public VoidTransactionCommand {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID must be valid");
        }
    }
}