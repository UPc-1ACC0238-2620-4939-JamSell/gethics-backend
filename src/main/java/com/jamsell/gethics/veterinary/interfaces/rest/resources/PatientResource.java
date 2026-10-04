package com.jamsell.gethics.veterinary.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResource(
        UUID patientId,
        String name,
        String tag,
        String breed,
        String status,
        int totalClinicalEvents,
        LocalDate lastEventDate,
        String lastEventType) {
}
