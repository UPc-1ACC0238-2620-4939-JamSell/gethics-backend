package com.jamsell.gethics.iot.domain.exceptions;

import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String code) {
        super("No se encontro un dispositivo vinculado con el codigo " + code + ".");
    }

    public DeviceNotFoundException(UUID deviceId) {
        super("No se encontro el dispositivo " + deviceId + ".");
    }
}
