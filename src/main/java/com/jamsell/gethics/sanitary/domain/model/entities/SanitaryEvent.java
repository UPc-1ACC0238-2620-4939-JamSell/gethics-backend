package com.jamsell.gethics.sanitary.domain.model.entities;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
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

    @Column(nullable = false)
    private LocalDateTime occurredAt;

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
}
