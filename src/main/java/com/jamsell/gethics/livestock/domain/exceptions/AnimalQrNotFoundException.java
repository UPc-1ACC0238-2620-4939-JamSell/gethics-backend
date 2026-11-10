package com.jamsell.gethics.livestock.domain.exceptions;

/** US-29, Escenario 2: el codigo escaneado no corresponde a ningun animal. */
public class AnimalQrNotFoundException extends RuntimeException {
    public AnimalQrNotFoundException() {
        super("No se encontro el registro: el codigo QR no corresponde a ningun animal.");
    }
}
