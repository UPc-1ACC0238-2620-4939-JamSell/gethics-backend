package com.jamsell.gethics.analytics.domain.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;

import java.util.List;
import java.util.UUID;

public interface AlertRepository {
    Alert save(Alert alert);

    boolean existsByTrendId(UUID trendId);

    /** Alertas PENDING (aun no enviadas), de la mas antigua a la mas reciente. */
    List<Alert> findPending();
}
