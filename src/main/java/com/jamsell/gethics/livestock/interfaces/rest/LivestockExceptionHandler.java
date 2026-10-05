package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalDataException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalWeightException;
import com.jamsell.gethics.shared.interfaces.rest.resources.ErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = AnimalController.class)
public class LivestockExceptionHandler {

    @ExceptionHandler(FutureBirthDateException.class)
    public ResponseEntity<ErrorResource> handleFutureBirthDate(FutureBirthDateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAnimalWeightException.class)
    public ResponseEntity<ErrorResource> handleInvalidWeight(InvalidAnimalWeightException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource(ex.getMessage()));
    }

    @ExceptionHandler(InvalidAnimalDataException.class)
    public ResponseEntity<ErrorResource> handleInvalidData(InvalidAnimalDataException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResource(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateAnimalTagException.class)
    public ResponseEntity<ErrorResource> handleDuplicateTag(DuplicateAnimalTagException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResource(ex.getMessage()));
    }
}
