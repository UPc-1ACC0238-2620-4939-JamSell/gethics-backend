package com.jamsell.gethics.sanitary.domain.exceptions;

public class InvalidCalendarPeriodException extends RuntimeException {
    public InvalidCalendarPeriodException() {
        super("Periodo de calendario inválido.");
    }
}
