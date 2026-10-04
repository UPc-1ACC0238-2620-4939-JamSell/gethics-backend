package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class ClientNotAssignedException extends RuntimeException {

    public ClientNotAssignedException(UUID veterinarianId, UUID clientId) {
        super("Client " + clientId + " is not assigned to veterinarian " + veterinarianId);
    }
}
