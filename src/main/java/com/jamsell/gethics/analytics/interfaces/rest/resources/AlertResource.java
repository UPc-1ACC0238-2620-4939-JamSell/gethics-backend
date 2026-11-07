package com.jamsell.gethics.analytics.interfaces.rest.resources;

import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;

import java.time.Instant;
import java.util.UUID;

/** {@code trendType} y {@code riskLevel} son los de la tendencia que origino la alerta. */
public record AlertResource(
        UUID id,
        UUID trendId,
        TrendType trendType,
        RiskLevel riskLevel,
        String message,
        AlertStatus status,
        Instant createdAt) {
}
