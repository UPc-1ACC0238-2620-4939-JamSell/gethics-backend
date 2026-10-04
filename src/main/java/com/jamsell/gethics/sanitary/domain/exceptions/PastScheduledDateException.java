package com.jamsell.gethics.sanitary.domain.exceptions;

public class PastScheduledDateException extends RuntimeException {
    public PastScheduledDateException() {
        super("La fecha programada no puede ser anterior a hoy.");
    }
}
