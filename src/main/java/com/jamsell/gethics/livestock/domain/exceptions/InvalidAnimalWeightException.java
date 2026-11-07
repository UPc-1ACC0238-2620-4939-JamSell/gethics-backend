package com.jamsell.gethics.livestock.domain.exceptions;

public class InvalidAnimalWeightException extends RuntimeException {
    public InvalidAnimalWeightException() {
        super("El peso inicial debe ser mayor a 0.");
    }
}
