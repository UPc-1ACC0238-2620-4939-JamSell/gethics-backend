package com.jamsell.gethics.veterinary.domain.model.valueobjects;

import java.util.UUID;

public record PatientInfo(UUID patientId, String name, String tag, String breed, String status) {
}
