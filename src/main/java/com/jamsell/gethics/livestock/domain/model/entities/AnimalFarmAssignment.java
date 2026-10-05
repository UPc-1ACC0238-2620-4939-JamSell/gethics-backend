package com.jamsell.gethics.livestock.domain.model.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Historial de granjas de un animal (US-10, escenario 2): una fila por cada asignacion o cambio. Nunca se modifica ni
 * se borra. {@code fromFarmId} es null en la primera asignacion del animal.
 */
@Entity
@Table(name = "animal_farm_assignments",
        indexes = @Index(name = "ix_animal_farm_assignments_animal", columnList = "animal_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalFarmAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Referencias por id dentro del mismo contexto, sin mapeo de relacion: el historial sobrevive a cualquier cambio.
    @Column(name = "animal_id", nullable = false, updatable = false)
    private UUID animalId;

    @Column(name = "from_farm_id", updatable = false)
    private UUID fromFarmId;

    @Column(name = "to_farm_id", nullable = false, updatable = false)
    private UUID toFarmId;

    @Column(nullable = false, updatable = false)
    private Instant assignedAt;

    public static AnimalFarmAssignment record(UUID animalId, UUID fromFarmId, UUID toFarmId, Instant assignedAt) {
        var assignment = new AnimalFarmAssignment();
        assignment.animalId = Objects.requireNonNull(animalId, "animalId");
        assignment.fromFarmId = fromFarmId;
        assignment.toFarmId = Objects.requireNonNull(toFarmId, "toFarmId");
        assignment.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
        return assignment;
    }
}
