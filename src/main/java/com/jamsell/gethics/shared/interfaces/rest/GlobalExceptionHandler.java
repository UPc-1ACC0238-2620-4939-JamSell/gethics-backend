package com.jamsell.gethics.shared.interfaces.rest;

import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.interfaces.rest.resources.ErrorResource;
import com.jamsell.gethics.shared.interfaces.rest.transform.ErrorResponseAssembler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResource> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.validationError(details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResource> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("El cuerpo de la petición no tiene un formato JSON válido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResource> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("El parámetro '" + ex.getName() + "' tiene un formato inválido"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResource> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("El archivo supera el tamaño máximo permitido de 2 MB"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResource> handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResource> handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.forbidden());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResource> handleException(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            var status = errorResponse.getStatusCode();
            var notFound = status.value() == 404;
            var resource = new ErrorResource(
                    notFound ? "RESOURCE_NOT_FOUND" : "REQUEST_ERROR",
                    notFound ? "El recurso solicitado no existe" : "La petición no pudo ser procesada",
                    null);
            return new ResponseEntity<>(resource, status);
        }
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.unexpected());
    }
}
