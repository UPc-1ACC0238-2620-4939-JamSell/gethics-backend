package com.jamsell.gethics.sanitary.domain.model.valueobjects;

// US-11 crea eventos COMPLETED; un SCHEDULED pasa a COMPLETED al registrarse como aplicado. CANCELLED aun no tiene comportamiento.
public enum SanitaryEventStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
