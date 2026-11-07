package com.jamsell.gethics.iot.domain.exceptions;

public class DuplicateDeviceCodeException extends RuntimeException {

    public DuplicateDeviceCodeException(String code) {
        super("Ya existe un dispositivo vinculado con el codigo " + code + ".");
    }
}
