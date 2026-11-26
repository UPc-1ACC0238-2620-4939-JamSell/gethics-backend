package com.jamsell.gethics.sanitary.domain.exceptions;

/** US-46: la ventana de alerta de eventos proximos solo admite de 1 a 30 dias. */
public class InvalidUpcomingWindowException extends RuntimeException {
    public InvalidUpcomingWindowException() {
        super("La ventana de alerta debe estar entre 1 y 30 dias.");
    }
}
