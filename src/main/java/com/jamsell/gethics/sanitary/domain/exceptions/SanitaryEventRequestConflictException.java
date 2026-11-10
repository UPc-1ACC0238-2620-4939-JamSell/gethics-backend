package com.jamsell.gethics.sanitary.domain.exceptions;

import java.util.UUID;

/** US-35: el clientRequestId ya identifica otro evento (otros datos, otro animal u otra operacion). */
public class SanitaryEventRequestConflictException extends RuntimeException {
    public SanitaryEventRequestConflictException(UUID clientRequestId) {
        super("El clientRequestId " + clientRequestId + " ya se uso con datos distintos.");
    }
}
