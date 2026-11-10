package com.jamsell.gethics.sanitary.domain.model.aggregates;

import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "clinical_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClinicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Referencia al animal de livestock por id: sin FK física entre bounded contexts.
    @Column(nullable = false, unique = true, updatable = false)
    private UUID animalId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "clinicalHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SanitaryEvent> events = new ArrayList<>();

    public ClinicalHistory(UUID animalId) {
        this.animalId = Objects.requireNonNull(animalId, "animalId");
        this.createdAt = this.updatedAt = Instant.now();
    }

    public SanitaryEvent registerEvent(SanitaryEventType type, LocalDateTime occurredAt, String description) {
        return registerEvent(type, occurredAt, description, null);
    }

    /** US-35: {@code clientRequestId} opcional (null = cliente sin idempotencia). */
    public SanitaryEvent registerEvent(SanitaryEventType type, LocalDateTime occurredAt, String description, UUID clientRequestId) {
        var event = new SanitaryEvent(this, type, occurredAt, description, clientRequestId);
        events.add(event);
        updatedAt = Instant.now();
        return event;
    }

    public SanitaryEvent scheduleEvent(SanitaryEventType type, LocalDate scheduledDate, String description, LocalDate today) {
        return scheduleEvent(type, scheduledDate, description, today, null);
    }

    /** US-35: {@code clientRequestId} opcional (null = cliente sin idempotencia). */
    public SanitaryEvent scheduleEvent(SanitaryEventType type, LocalDate scheduledDate, String description, LocalDate today,
                                       UUID clientRequestId) {
        var event = SanitaryEvent.schedule(this, type, scheduledDate, description, today, clientRequestId);
        events.add(event);
        updatedAt = Instant.now();
        return event;
    }

    public SanitaryEvent completeScheduledEvent(UUID eventId, LocalDateTime occurredAt, String description, LocalDate today) {
        var event = findEvent(eventId);
        event.complete(occurredAt, description, today);
        updatedAt = Instant.now();
        return event;
    }

    /** US-33: solo un evento de ESTE historial (del animal indicado) puede cancelarse; otro id es "no encontrado". */
    public SanitaryEvent cancelScheduledEvent(UUID eventId) {
        var event = findEvent(eventId);
        event.cancel();
        updatedAt = Instant.now();
        return event;
    }

    private SanitaryEvent findEvent(UUID eventId) {
        return events.stream()
                .filter(e -> e.getId() != null && e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(SanitaryEventNotFoundException::new);
    }

    /**
     * El animal dejo de estar activo (baja o venta): cancela todos sus eventos SCHEDULED para que salgan del
     * calendario y no generen recordatorios. Los COMPLETED y CANCELLED no cambian. Devuelve cuantos cancelo.
     */
    public int cancelAllScheduledEvents() {
        int cancelled = 0;
        for (var event : events) {
            if (event.getStatus() == com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus.SCHEDULED) {
                event.cancel();
                cancelled++;
            }
        }
        if (cancelled > 0) {
            updatedAt = Instant.now();
        }
        return cancelled;
    }
}
