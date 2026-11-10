package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class PatientNotFoundException extends CareRejectedException {
    public PatientNotFoundException(UUID patientId) {
        super("Patient " + patientId + " does not exist");
    }
}
