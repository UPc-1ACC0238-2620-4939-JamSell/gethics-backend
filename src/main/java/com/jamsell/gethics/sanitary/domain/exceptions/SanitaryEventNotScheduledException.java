package com.jamsell.gethics.sanitary.domain.exceptions;

public class SanitaryEventNotScheduledException extends RuntimeException {
    public SanitaryEventNotScheduledException() {
        super("Solo un evento programado (SCHEDULED) puede registrarse como aplicado.");
    }
}
