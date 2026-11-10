package com.jamsell.gethics.veterinary.interfaces.rest;

import com.jamsell.gethics.veterinary.domain.model.exceptions.AssignmentNotFoundException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.ClinicalHistorySyncException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientAccessDeniedException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientNotActiveException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientNotFoundException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientWithoutClientException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del modulo veterinario con el mismo formato {@code {"message": ...}} que usan sus controladores. */
@RestControllerAdvice(basePackageClasses = VeterinaryCareController.class)
public class VeterinaryExceptionHandler {

    @ExceptionHandler({PatientNotFoundException.class, AssignmentNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException exception) {
        return body(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler({PatientNotActiveException.class, PatientWithoutClientException.class})
    public ResponseEntity<Map<String, String>> handlePatientConflict(RuntimeException exception) {
        return body(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(PatientAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(PatientAccessDeniedException exception) {
        return body(HttpStatus.FORBIDDEN, exception);
    }

    /** La atencion quedo guardada; reintentar la misma solicitud termina de actualizar el historial. */
    @ExceptionHandler(ClinicalHistorySyncException.class)
    public ResponseEntity<Map<String, String>> handleHistoryUnavailable(ClinicalHistorySyncException exception) {
        return body(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    private static ResponseEntity<Map<String, String>> body(HttpStatus status, RuntimeException exception) {
        return ResponseEntity.status(status).body(Map.of("message", exception.getMessage()));
    }
}
