package com.jamsell.gethics.iot.interfaces.rest;

import com.jamsell.gethics.iot.domain.exceptions.DeviceNotFoundException;
import com.jamsell.gethics.iot.domain.exceptions.DuplicateDeviceCodeException;
import com.jamsell.gethics.shared.interfaces.rest.resources.ErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackageClasses = DeviceController.class)
public class IotExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResource> handleNotFound(DeviceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResource(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateDeviceCodeException.class)
    public ResponseEntity<ErrorResource> handleDuplicate(DuplicateDeviceCodeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResource(ex.getMessage()));
    }

    // Sin esto, el handler de IllegalArgumentException (causa de la conversion) respondia con el texto interno de Spring.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResource> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource("Parametro invalido: " + ex.getName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResource> handleInvalidArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource(ex.getMessage()));
    }
}
