package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.livestock.interfaces.acl.LivestockContextFacade;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.PatientAccessLookup;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientAccess;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** El paciente es un animal de Livestock: su estado y su dueno (cliente) salen de la fachada. */
@Component
public class LivestockPatientAccessAdapter implements PatientAccessLookup {

    private final LivestockContextFacade livestock;

    public LivestockPatientAccessAdapter(LivestockContextFacade livestock) {
        this.livestock = livestock;
    }

    @Override
    public Optional<PatientAccess> findByPatientId(UUID patientId) {
        return livestock.findAnimalAccess(patientId)
                .map(access -> new PatientAccess(access.animalId(), access.active(), access.ownerId()));
    }
}
