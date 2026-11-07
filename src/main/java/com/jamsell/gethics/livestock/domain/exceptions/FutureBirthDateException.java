package com.jamsell.gethics.livestock.domain.exceptions;

public class FutureBirthDateException extends RuntimeException {
    public FutureBirthDateException() {
        super("La fecha de nacimiento no puede ser posterior a hoy.");
    }
}
