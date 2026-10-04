package com.jamsell.gethics.sanitary.domain.exceptions;

public class SanitaryEventNotFoundException extends RuntimeException {
    public SanitaryEventNotFoundException() {
        super("El evento sanitario no existe para este animal.");
    }
}
