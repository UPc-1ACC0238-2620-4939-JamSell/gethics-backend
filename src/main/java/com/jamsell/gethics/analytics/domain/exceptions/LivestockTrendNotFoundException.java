package com.jamsell.gethics.analytics.domain.exceptions;

import java.util.UUID;

public class LivestockTrendNotFoundException extends RuntimeException {
    public LivestockTrendNotFoundException(UUID trendId) {
        super("No existe la tendencia " + trendId + ".");
    }
}
