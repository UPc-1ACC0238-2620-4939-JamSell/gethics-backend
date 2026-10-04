package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientInfo;
import java.util.List;
import java.util.UUID;

public interface ClientPatientsLookup {

    List<PatientInfo> findPatientsByClientId(UUID clientId);
}
