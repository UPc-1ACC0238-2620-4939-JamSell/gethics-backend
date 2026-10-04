package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientPatientsLookup;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Temporary adapter. Replace its body with a call to the livestock context facade
 * once it exposes the animals of a client.
 */
@Component
public class LivestockPatientsAdapter implements ClientPatientsLookup {

    @Override
    public List<PatientInfo> findPatientsByClientId(UUID clientId) {
        return List.of();
    }
}
