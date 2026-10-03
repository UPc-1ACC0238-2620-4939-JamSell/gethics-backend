package com.jamsell.gethics.analytics.domain.model.commands;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de analisis ya CLASIFICADO por quien lo produzca (el clasificador real aun no existe): ambos niveles de
 * riesgo llegan calculados y {@code alertMessage} se usa solo si la politica determina que corresponde una alerta.
 */
public record RegisterClassifiedTrendCommand(
        UUID ownerId,
        RiskLevel overallRiskLevel,
        TrendType trendType,
        RiskLevel trendRiskLevel,
        String description,
        Instant detectedAt,
        String alertMessage) {
}
