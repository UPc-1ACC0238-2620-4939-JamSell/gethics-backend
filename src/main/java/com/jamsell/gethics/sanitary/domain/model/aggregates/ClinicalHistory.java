package com.jamsell.gethics.sanitary.domain.model.aggregates;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
        var event = new SanitaryEvent(this, type, occurredAt, description);
        events.add(event);
        updatedAt = Instant.now();
        return event;
    }
}
