package com.jamsell.gethics.analytics.domain.repositories;

import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;

import java.util.Optional;
import java.util.UUID;

/** Puerto de lectura: localiza una tendencia por id para generar su alerta sin cargar el aggregate completo. */
public interface LivestockTrendRepository {
    Optional<LivestockTrend> findById(UUID trendId);
}
