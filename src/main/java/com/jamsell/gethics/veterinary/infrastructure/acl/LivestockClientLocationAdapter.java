package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientLocationLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LivestockClientLocationAdapter implements ClientLocationLookup {

    @Override
    public Optional<String> findLocationByClientId(UUID clientId) {
        return Optional.empty();
    }
}