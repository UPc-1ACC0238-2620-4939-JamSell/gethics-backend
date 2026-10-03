package com.jamsell.gethics.shared.interfaces.rest.transform;

import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.interfaces.rest.resources.ErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public final class ErrorResponseAssembler {

    private ErrorResponseAssembler() {
    }

    public static ResponseEntity<ErrorResource> toErrorResponseFromApplicationError(ApplicationError error) {
        var resource = new ErrorResource(error.code(), error.message(), error.details());
        return new ResponseEntity<>(resource, toStatusFromErrorCode(error.code()));
    }

    public static HttpStatusCode toStatusFromErrorCode(String errorCode) {
        return switch (errorCode) {
            case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "INVALID_CREDENTIALS", "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "PAYMENT_REJECTED" -> HttpStatus.PAYMENT_REQUIRED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "BUSINESS_RULE_VIOLATION" -> HttpStatusCode.valueOf(422);
            case String code when code.endsWith("_NOT_FOUND") -> HttpStatus.NOT_FOUND;
            case String code when code.endsWith("_CONFLICT") -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
