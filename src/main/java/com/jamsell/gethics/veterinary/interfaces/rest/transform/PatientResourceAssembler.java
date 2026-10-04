package com.jamsell.gethics.veterinary.interfaces.rest.transform;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientView;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.PatientResource;

public final class PatientResourceAssembler {

    private PatientResourceAssembler() {
    }

    public static PatientResource toResource(PatientView view) {
        var patient = view.patient();
        var summary = view.clinicalSummary();
        return new PatientResource(
                patient.patientId(),
                patient.name(),
                patient.tag(),
                patient.breed(),
                patient.status(),
                summary == null ? 0 : summary.totalEvents(),
                summary == null ? null : summary.lastEventDate(),
                summary == null ? null : summary.lastEventType());
    }
}
