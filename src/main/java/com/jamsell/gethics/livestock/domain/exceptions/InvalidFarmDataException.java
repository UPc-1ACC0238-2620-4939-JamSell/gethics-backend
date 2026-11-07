package com.jamsell.gethics.livestock.domain.exceptions;

public class InvalidFarmDataException extends RuntimeException {
    public InvalidFarmDataException(String message) {
        super(message);
    }
}
