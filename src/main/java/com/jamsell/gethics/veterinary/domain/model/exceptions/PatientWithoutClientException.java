package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class PatientWithoutClientException extends CareRejectedException {
    public PatientWithoutClientException(UUID patientId) {
        super("Patient " + patientId + " is not linked to a farm, so its client cannot be determined");
    }
}
