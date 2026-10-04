package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import java.util.Optional;
import java.util.UUID;

public interface ClientLocationLookup {

    Optional<String> findLocationByClientId(UUID clientId);
}

