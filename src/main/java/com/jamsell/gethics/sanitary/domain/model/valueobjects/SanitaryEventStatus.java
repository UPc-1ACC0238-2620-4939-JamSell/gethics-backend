package com.jamsell.gethics.sanitary.domain.model.valueobjects;

// US-11 solo crea eventos COMPLETED; SCHEDULED y CANCELLED aun no tienen comportamiento.
public enum SanitaryEventStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}
