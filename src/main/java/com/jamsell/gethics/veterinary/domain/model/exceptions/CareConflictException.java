package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class CareConflictException extends RuntimeException {

    public CareConflictException(UUID clientRequestId) {
        super("Request " + clientRequestId + " was already used with different content");
    }
}
