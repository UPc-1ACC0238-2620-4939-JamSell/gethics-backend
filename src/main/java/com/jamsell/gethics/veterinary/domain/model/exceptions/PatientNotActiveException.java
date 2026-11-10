package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class PatientNotActiveException extends CareRejectedException {
    public PatientNotActiveException(UUID patientId) {
        super("Patient " + patientId + " is no longer active (deactivated, sold or deceased)");
    }
}
