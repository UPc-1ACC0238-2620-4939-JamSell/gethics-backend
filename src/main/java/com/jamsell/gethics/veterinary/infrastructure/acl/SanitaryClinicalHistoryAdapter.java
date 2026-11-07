package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalHistoryWriter;
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import org.springframework.stereotype.Component;

/**
 * Temporary adapter. Replace its body with a call to the sanitary context facade
 * once it exposes a command to append a veterinary care event to the clinical history.
 */
@Component
public class SanitaryClinicalHistoryAdapter implements ClinicalHistoryWriter {

    @Override
    public void append(CareRecord record) {
        // Intentionally empty until the sanitary facade is available.
    }
}
