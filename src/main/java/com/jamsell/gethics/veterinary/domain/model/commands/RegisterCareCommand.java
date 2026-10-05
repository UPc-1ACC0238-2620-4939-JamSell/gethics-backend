package com.jamsell.gethics.veterinary.domain.model.commands;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterCareCommand(
        UUID clientRequestId,
        UUID veterinarianId,
        UUID patientId,
        String diagnosis,
        String treatment,
        LocalDate nextControlDate,
        Instant occurredAt) {
}
