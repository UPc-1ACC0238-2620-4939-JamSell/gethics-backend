package com.jamsell.gethics.analytics.domain.model.aggregates;

import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root: un unico Analytics por propietario (owner_id UNIQUE segun el Database Design). {@code ownerId} es una
 * referencia al propietario de IAM por id: sin FK fisica entre bounded contexts.
 * <p>
 * El riesgo general ({@code riskLevel}) NO se deriva de las tendencias: el informe no define como se calcula, asi que se
 * recibe ya calculado junto con cada resultado clasificado. Es distinto del riesgo de cada {@link LivestockTrend}, que
 * es el que se evalua para decidir si se genera una alerta.
 * <p>
 * Timestamps: {@code createdAt}, {@code lastAnalysisAt} (y {@code detectedAt} de la tendencia) son {@link Instant}, la
 * convencion del backend para marcas de tiempo tecnicas (ClinicalHistory, SanitaryEvent y Reminder usan Instant para
 * createdAt/updatedAt/sentAt; LocalDateTime/LocalDate se reservan para fechas de negocio que aporta el usuario). El
 * informe solo dice DATETIME: usar Instant (timestamp with time zone en PostgreSQL) es una decision tecnica de
 * implementacion.
 */
@Entity
@Table(name = "analytics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private Instant lastAnalysisAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "analytics", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LivestockTrend> trends = new ArrayList<>();

    public Analytics(UUID ownerId, RiskLevel riskLevel) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        this.createdAt = this.lastAnalysisAt = Instant.now();
    }

    /** Registra una tendencia ya clasificada y fija el riesgo general recibido; ambos niveles son independientes. */
    public LivestockTrend registerTrend(TrendType type, RiskLevel trendRiskLevel, String description, Instant detectedAt,
                                        RiskLevel overallRiskLevel) {
        var trend = new LivestockTrend(this, type, description, detectedAt, trendRiskLevel);
        this.riskLevel = Objects.requireNonNull(overallRiskLevel, "overallRiskLevel");
        this.lastAnalysisAt = Instant.now();
        trends.add(trend);
        return trend;
    }
}
