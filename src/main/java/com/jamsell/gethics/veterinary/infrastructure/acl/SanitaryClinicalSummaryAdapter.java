package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalSummaryLookup;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Temporary adapter. Replace its body with a call to the sanitary context facade
 * once it exposes a clinical history summary per animal.
 */
@Component
public class SanitaryClinicalSummaryAdapter implements ClinicalSummaryLookup {

    @Override
    public Optional<ClinicalSummary> findSummaryByPatientId(UUID patientId) {
        return Optional.empty();
    }
}
