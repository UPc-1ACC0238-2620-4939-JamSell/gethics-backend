package com.jamsell.gethics.finance.domain.model.queries;

import java.util.UUID;

public record GetTransactionsByOwnerQuery(UUID ownerId) {
}
