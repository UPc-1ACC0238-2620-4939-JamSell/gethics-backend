package com.jamsell.gethics.sanitary.domain.model.entities;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.PastScheduledDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotScheduledException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sanitary_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SanitaryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinical_history_id", nullable = false, updatable = false)
    private ClinicalHistory clinicalHistory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SanitaryEventType type;

    // Solo eventos COMPLETED; null mientras el evento esta SCHEDULED.
    private LocalDateTime occurredAt;

    // Fecha planificada: la tienen los eventos programados y se conserva al completarlos; null en los registrados por US-11.
    private LocalDate scheduledDate;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SanitaryEventStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public SanitaryEvent(ClinicalHistory clinicalHistory, SanitaryEventType type, LocalDateTime occurredAt, String description) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (occurredAt.toLocalDate().isAfter(LocalDate.now())) {
            throw new FutureEventDateException();
        }
        this.clinicalHistory = clinicalHistory;
        this.type = type;
        this.occurredAt = occurredAt;
        this.description = description;
        this.status = SanitaryEventStatus.COMPLETED;
        this.createdAt = Instant.now();
    }

    /** {@code today} lo aporta Application desde el Clock: un evento no puede programarse en una fecha ya vencida. */
    public static SanitaryEvent schedule(ClinicalHistory clinicalHistory, SanitaryEventType type, LocalDate scheduledDate,
                                         String description, LocalDate today) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scheduledDate, "scheduledDate");
        if (scheduledDate.isBefore(Objects.requireNonNull(today, "today"))) {
            throw new PastScheduledDateException();
        }
        var event = new SanitaryEvent();
        event.clinicalHistory = clinicalHistory;
        event.type = type;
        event.scheduledDate = scheduledDate;
        event.description = description;
        event.status = SanitaryEventStatus.SCHEDULED;
        event.createdAt = Instant.now();
        return event;
    }

    /**
     * Registra como aplicado ESTE evento programado (SCHEDULED -> COMPLETED): no crea otro evento y conserva
     * {@code scheduledDate}. Sin {@code description} se mantiene la de la programacion.
     */
    public void complete(LocalDateTime occurredAt, String description, LocalDate today) {
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (status != SanitaryEventStatus.SCHEDULED) {
            throw new SanitaryEventNotScheduledException();
        }
        if (occurredAt.toLocalDate().isAfter(Objects.requireNonNull(today, "today"))) {
            throw new FutureEventDateException();
        }
        this.occurredAt = occurredAt;
        if (description != null) {
            this.description = description;
        }
        this.status = SanitaryEventStatus.COMPLETED;
    }
}
