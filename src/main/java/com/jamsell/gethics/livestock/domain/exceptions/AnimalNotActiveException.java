package com.jamsell.gethics.livestock.domain.exceptions;

/** El animal ya no esta ACTIVE (inactivo, vendido o fallecido), asi que no se puede dar de baja de nuevo. */
public class AnimalNotActiveException extends RuntimeException {
    public AnimalNotActiveException() {
        super("El animal ya esta dado de baja.");
    }
}
