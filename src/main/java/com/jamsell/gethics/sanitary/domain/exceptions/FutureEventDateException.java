package com.jamsell.gethics.sanitary.domain.exceptions;

public class FutureEventDateException extends RuntimeException {
    public FutureEventDateException() {
        super("La fecha del evento no puede ser posterior a hoy.");
    }
}
