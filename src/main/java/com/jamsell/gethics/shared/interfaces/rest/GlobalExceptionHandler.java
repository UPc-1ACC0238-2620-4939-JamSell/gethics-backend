package com.jamsell.gethics.shared.interfaces.rest;

import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.interfaces.rest.resources.ErrorResource;
import com.jamsell.gethics.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Errores transversales que no cubre {@link RestExceptionHandler}: rol insuficiente en {@code @PreAuthorize}
 * (403) y fotos de perfil demasiado grandes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResource> handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.forbidden());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResource> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.validationError("El archivo supera el tamaño máximo permitido de 2 MB"));
    }
}
