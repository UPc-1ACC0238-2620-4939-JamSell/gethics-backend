package com.jamsell.gethics.veterinary.domain.model.aggregates;

import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "care_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_request_id", nullable = false, unique = true)
    private UUID clientRequestId;

    @Column(name = "veterinarian_id", nullable = false)
    private UUID veterinarianId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String treatment;

    @Column(name = "next_control_date")
    private LocalDate nextControlDate;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "history_synced", nullable = false)
    private boolean historySynced;

    public CareRecord(RegisterCareCommand command) {
        if (command.clientRequestId() == null || command.veterinarianId() == null
                || command.patientId() == null) {
            throw new IllegalArgumentException("Request id, veterinarian id and patient id are required");
        }
        if (isBlank(command.diagnosis()) || isBlank(command.treatment())) {
            throw new IllegalArgumentException("Diagnosis and treatment are required");
        }
        var now = Instant.now();
        var occurred = command.occurredAt() == null ? now : command.occurredAt();
        if (occurred.isAfter(now.plus(5, ChronoUnit.MINUTES))) {
            throw new IllegalArgumentException("Care date cannot be in the future");
        }
        if (command.nextControlDate() != null
                && command.nextControlDate().isBefore(LocalDate.ofInstant(occurred, ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Next control cannot be before the care date");
        }
        this.clientRequestId = command.clientRequestId();
        this.veterinarianId = command.veterinarianId();
        this.patientId = command.patientId();
        this.diagnosis = command.diagnosis();
        this.treatment = command.treatment();
        this.nextControlDate = command.nextControlDate();
        this.occurredAt = occurred;
        this.receivedAt = now;
        this.historySynced = false;
    }

    public boolean matches(RegisterCareCommand command) {
        return veterinarianId.equals(command.veterinarianId())
                && patientId.equals(command.patientId())
                && diagnosis.equals(command.diagnosis())
                && treatment.equals(command.treatment())
                && Objects.equals(nextControlDate, command.nextControlDate());
    }

    public void markHistorySynced() {
        this.historySynced = true;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
