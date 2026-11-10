package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.livestock.interfaces.acl.LivestockContextFacade;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientPatientsLookup;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** TS07: los pacientes de un cliente son sus animales activos, leidos por la fachada de Livestock. */
@Component
public class LivestockPatientsAdapter implements ClientPatientsLookup {

    private final LivestockContextFacade livestock;

    public LivestockPatientsAdapter(LivestockContextFacade livestock) {
        this.livestock = livestock;
    }

    @Override
    public List<PatientInfo> findPatientsByClientId(UUID clientId) {
        return livestock.findActiveAnimalsByOwnerId(clientId).stream()
                .map(animal -> new PatientInfo(animal.id(), animal.name(), animal.tag(), animal.breed(),
                        animal.status()))
                .toList();
    }
}
