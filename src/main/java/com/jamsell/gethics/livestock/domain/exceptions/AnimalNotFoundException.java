package com.jamsell.gethics.livestock.domain.exceptions;

public class AnimalNotFoundException extends RuntimeException {
    public AnimalNotFoundException() {
        super("El animal no existe.");
    }
}
