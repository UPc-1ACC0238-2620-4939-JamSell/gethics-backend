package com.jamsell.gethics.sanitary.application.internal.outboundservices;

import java.time.LocalDate;
import java.util.UUID;

public record VaccinationReminderNotification(
        UUID sanitaryEventId,
        UUID animalId,
        LocalDate vaccinationDate,
        String description) {
}
