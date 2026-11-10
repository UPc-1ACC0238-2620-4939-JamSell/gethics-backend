package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.livestock.interfaces.acl.LivestockContextFacade;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientLocationLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** TS07: ubicacion del cliente tomada de la fachada de Livestock (primera granja del ganadero). */
@Component
public class LivestockClientLocationAdapter implements ClientLocationLookup {

    private final LivestockContextFacade livestock;

    public LivestockClientLocationAdapter(LivestockContextFacade livestock) {
        this.livestock = livestock;
    }

    @Override
    public Optional<String> findLocationByClientId(UUID clientId) {
        return livestock.findLocationByOwnerId(clientId);
    }
}
