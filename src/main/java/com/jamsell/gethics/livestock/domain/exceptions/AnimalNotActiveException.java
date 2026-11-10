package com.jamsell.gethics.livestock.domain.exceptions;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

/** El animal ya no esta ACTIVE (dado de baja, vendido o fallecido), asi que no admite mas cambios. */
public class AnimalNotActiveException extends RuntimeException {
    public AnimalNotActiveException() {
        this(AnimalStatus.INACTIVE);
    }

    public AnimalNotActiveException(AnimalStatus status) {
        super(messageFor(status));
    }

    private static String messageFor(AnimalStatus status) {
        if (status == AnimalStatus.SOLD) {
            return "El animal ya fue vendido.";
        }
        if (status == AnimalStatus.DECEASED) {
            return "El animal esta registrado como fallecido.";
        }
        return "El animal ya esta dado de baja.";
    }
}
