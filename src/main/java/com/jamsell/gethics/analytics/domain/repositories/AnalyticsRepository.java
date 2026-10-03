package com.jamsell.gethics.analytics.domain.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;

import java.util.Optional;
import java.util.UUID;

public interface AnalyticsRepository {
    Optional<Analytics> findByOwnerId(UUID ownerId);

    /** Persiste el aggregate (incluidas sus tendencias) y devuelve la instancia persistida. */
    Analytics save(Analytics analytics);
}
