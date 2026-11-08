package com.jamsell.gethics.shared.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Cuerpo de error comun. Los modulos existentes siguen usando {@code new ErrorResource(message)} (el JSON solo
 * lleva {@code message}); IAM usa ademas {@code code} y {@code details}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResource(String code, String message, String details) {

    public ErrorResource(String message) {
        this(null, message, null);
    }
}
