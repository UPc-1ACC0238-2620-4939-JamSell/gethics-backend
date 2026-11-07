package com.jamsell.gethics.iot.domain.exceptions;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String code) {
        super("No se encontro un dispositivo vinculado con el codigo " + code + ".");
    }
}
