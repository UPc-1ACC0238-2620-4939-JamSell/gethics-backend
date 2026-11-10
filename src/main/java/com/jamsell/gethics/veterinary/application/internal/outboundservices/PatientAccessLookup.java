package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientAccess;
import java.util.Optional;
import java.util.UUID;

/** Puerto hacia Livestock: existe el paciente, esta activo y de que cliente es. */
public interface PatientAccessLookup {

    Optional<PatientAccess> findByPatientId(UUID patientId);
}
