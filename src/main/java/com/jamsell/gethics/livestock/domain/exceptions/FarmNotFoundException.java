package com.jamsell.gethics.livestock.domain.exceptions;

public class FarmNotFoundException extends RuntimeException {
    public FarmNotFoundException() {
        super("La granja no existe.");
    }
}
