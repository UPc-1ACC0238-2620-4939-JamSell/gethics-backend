package com.jamsell.gethics.sanitary.domain.exceptions;

public class SanitaryEventNotScheduledException extends RuntimeException {
    public SanitaryEventNotScheduledException() {
        super("Solo un evento programado (SCHEDULED) puede registrarse como aplicado.");
    }

    private SanitaryEventNotScheduledException(String message) {
        super(message);
    }

    /** US-33: mismo 409 que al completar, con el mensaje propio de la cancelacion. */
    public static SanitaryEventNotScheduledException forCancellation() {
        return new SanitaryEventNotScheduledException("Solo un evento programado (SCHEDULED) puede cancelarse.");
    }

    /** US-47: mismo 409, con el mensaje propio de la reprogramacion. */
    public static SanitaryEventNotScheduledException forReschedule() {
        return new SanitaryEventNotScheduledException("Solo un evento programado (SCHEDULED) puede reprogramarse.");
    }
}
