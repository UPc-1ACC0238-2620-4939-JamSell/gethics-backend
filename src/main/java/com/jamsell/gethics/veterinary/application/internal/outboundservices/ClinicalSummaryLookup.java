package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalSummaryLookup {

    Optional<ClinicalSummary> findSummaryByPatientId(UUID patientId);
}
