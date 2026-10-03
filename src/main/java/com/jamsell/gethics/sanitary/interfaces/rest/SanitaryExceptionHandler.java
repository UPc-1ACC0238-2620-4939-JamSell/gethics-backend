package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.InvalidCalendarPeriodException;
import com.jamsell.gethics.shared.interfaces.rest.resources.ErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = SanitaryEventController.class)
public class SanitaryExceptionHandler {

    @ExceptionHandler(FutureEventDateException.class)
    public ResponseEntity<ErrorResource> handleFutureDate(FutureEventDateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCalendarPeriodException.class)
    public ResponseEntity<ErrorResource> handleInvalidPeriod(InvalidCalendarPeriodException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource(ex.getMessage()));
    }
}
