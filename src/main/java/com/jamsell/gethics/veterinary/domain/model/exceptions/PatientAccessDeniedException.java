package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class PatientAccessDeniedException extends CareRejectedException {
    public PatientAccessDeniedException(UUID veterinarianId, UUID patientId) {
        super("Veterinarian " + veterinarianId + " is not assigned to the client of patient " + patientId);
    }
}
