package com.jamsell.gethics.sanitary.domain.model.entities;

import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Recordatorio de un {@link SanitaryEvent} (un evento puede tener 0..* recordatorios). La restriccion unica
 * (sanitary_event_id, scheduled_for) es la clave de idempotencia: un mismo recordatorio no se crea dos veces, pero el
 * modelo permite otras anticipaciones (otro scheduled_for) para el mismo evento.
 * <p>
 * {@code attempts} y {@code sentAt} son atributos tecnicos agregados para gestionar entrega y reintentos; no figuran
 * en el Database Design del informe. La relacion con el evento genera la FK real hacia sanitary_events (mismo
 * bounded context, a diferencia de animalId/livestock).
 */
@Entity
@Table(name = "reminders", uniqueConstraints = @UniqueConstraint(
        name = "uk_reminders_event_scheduled_for", columnNames = {"sanitary_event_id", "scheduled_for"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sanitary_event_id", nullable = false, updatable = false)
    private SanitaryEvent sanitaryEvent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status;

    // Tecnico: intentos de entrega realizados (exitosos o fallidos).
    @Column(nullable = false)
    private int attempts;

    // Tecnico: momento de la entrega exitosa.
    private Instant sentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Reminder create(SanitaryEvent sanitaryEvent, LocalDateTime scheduledFor) {
        var reminder = new Reminder();
        reminder.sanitaryEvent = Objects.requireNonNull(sanitaryEvent, "sanitaryEvent");
        reminder.scheduledFor = Objects.requireNonNull(scheduledFor, "scheduledFor");
        reminder.status = ReminderStatus.PENDING;
        reminder.createdAt = Instant.now();
        return reminder;
    }

    public UUID getSanitaryEventId() {
        return sanitaryEvent.getId();
    }

    public void markSent(Instant sentAt) {
        requireNotSent();
        this.status = ReminderStatus.SENT;
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt");
        this.attempts++;
    }

    public void markFailed() {
        requireNotSent();
        this.status = ReminderStatus.FAILED;
        this.attempts++;
    }

    private void requireNotSent() {
        if (status == ReminderStatus.SENT) {
            throw new IllegalStateException("El recordatorio ya fue enviado.");
        }
    }
}
