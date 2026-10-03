package com.jamsell.gethics.shared.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResource(String code, String message, String details) {
}
