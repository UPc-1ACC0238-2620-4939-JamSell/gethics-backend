package com.jamsell.gethics.analytics.domain.model.entities;

import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tendencia ya CLASIFICADA (tipo y nivel de riesgo recibidos de fuera). Esta clase no calcula nada: el algoritmo de
 * deteccion y la clasificacion de riesgo siguen pendientes de definicion.
 * <p>
 * {@code riskLevel} es el riesgo de ESTA tendencia concreta (no el riesgo general del {@code Analytics}) y es el que se
 * evalua para decidir si se genera una alerta. {@code description} es TEXT NOT NULL en el informe, sin longitud maxima.
 * {@code detectedAt} es {@link Instant} por la convencion tecnica del backend (ver {@code Analytics}).
 */
@Entity
@Table(name = "livestock_trends")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LivestockTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK real hacia analytics (mismo bounded context).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analytics_id", nullable = false, updatable = false)
    private Analytics analytics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrendType type;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    public LivestockTrend(Analytics analytics, TrendType type, String description, Instant detectedAt, RiskLevel riskLevel) {
        this.analytics = Objects.requireNonNull(analytics, "analytics");
        this.type = Objects.requireNonNull(type, "type");
        this.description = Objects.requireNonNull(description, "description");
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
    }

    public UUID getAnalyticsId() {
        return analytics.getId();
    }
}
