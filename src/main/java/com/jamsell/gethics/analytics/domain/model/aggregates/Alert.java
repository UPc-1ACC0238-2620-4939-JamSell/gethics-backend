package com.jamsell.gethics.analytics.domain.model.aggregates;

import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Alerta de una tendencia (una tendencia puede tener 0..* alertas). {@code ownerId} es referencia a IAM por id, sin FK;
 * {@code trend} genera la FK real hacia livestock_trends. El texto del mensaje lo aporta el productor del resultado
 * clasificado: el informe no define su contenido; es TEXT NOT NULL, sin longitud maxima.
 */
@Entity
@Table(name = "alerts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trend_id", nullable = false, updatable = false)
    private LivestockTrend trend;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Alert create(UUID ownerId, LivestockTrend trend, String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje de la alerta es obligatorio.");
        }
        var alert = new Alert();
        alert.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        alert.trend = Objects.requireNonNull(trend, "trend");
        alert.message = message;
        alert.status = AlertStatus.PENDING;
        alert.createdAt = Instant.now();
        return alert;
    }

    public UUID getTrendId() {
        return trend.getId();
    }

    public void markSent() {
        if (status != AlertStatus.PENDING) {
            throw new IllegalStateException("Solo una alerta PENDING puede marcarse como enviada.");
        }
        this.status = AlertStatus.SENT;
    }
}
