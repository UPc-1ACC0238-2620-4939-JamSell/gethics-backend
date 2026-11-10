package com.jamsell.gethics.analytics.domain.exceptions;

/** US-39 Escenario 2: el periodo pedido no tiene informacion que exportar. El mensaje se muestra tal cual en la app. */
public class ReportDataNotAvailableException extends RuntimeException {

    public static final String MESSAGE = "No hay información disponible para el periodo seleccionado";

    public ReportDataNotAvailableException() {
        super(MESSAGE);
    }
}
