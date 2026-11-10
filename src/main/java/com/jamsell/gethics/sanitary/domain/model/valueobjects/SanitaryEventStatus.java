package com.jamsell.gethics.sanitary.domain.model.valueobjects;

// US-11 crea eventos COMPLETED; un SCHEDULED pasa a COMPLETED al registrarse como aplicado o a CANCELLED al cancelarse (US-33).
// COMPLETED y CANCELLED son finales.
public enum SanitaryEventStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
