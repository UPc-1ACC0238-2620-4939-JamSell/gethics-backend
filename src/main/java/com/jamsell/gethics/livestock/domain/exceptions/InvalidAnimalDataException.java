package com.jamsell.gethics.livestock.domain.exceptions;

public class InvalidAnimalDataException extends RuntimeException {
    public InvalidAnimalDataException(String message) {
        super(message);
    }
}
