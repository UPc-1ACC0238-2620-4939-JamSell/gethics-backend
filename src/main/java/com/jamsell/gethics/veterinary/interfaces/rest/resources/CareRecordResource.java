package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CareRecordResource(
        UUID id,
        UUID clientRequestId,
        UUID veterinarianId,
        UUID patientId,
        String diagnosis,
        String treatment,
        LocalDate nextControlDate,
        Instant occurredAt,
        boolean historySynced) {
}
