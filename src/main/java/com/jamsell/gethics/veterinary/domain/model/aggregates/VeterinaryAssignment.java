package com.jamsell.gethics.veterinary.domain.model.aggregates;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "veterinary_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VeterinaryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "veterinarian_id", nullable = false)
    private UUID veterinarianId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VeterinaryAssignmentStatus status;

    public VeterinaryAssignment(UUID veterinarianId, UUID clientId) {
        if (veterinarianId == null || clientId == null) {
            throw new IllegalArgumentException("Veterinarian id and client id are required");
        }
        if (veterinarianId.equals(clientId)) {
            throw new IllegalArgumentException("A veterinarian cannot be assigned to themselves");
        }
        this.veterinarianId = veterinarianId;
        this.clientId = clientId;
        this.assignedAt = Instant.now();
        this.status = VeterinaryAssignmentStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = VeterinaryAssignmentStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == VeterinaryAssignmentStatus.ACTIVE;
    }
}

