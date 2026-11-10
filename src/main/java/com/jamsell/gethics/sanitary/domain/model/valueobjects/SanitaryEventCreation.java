package com.jamsell.gethics.sanitary.domain.model.valueobjects;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

/** US-35 (como CareRegistration de US-19): {@code created} = false cuando un reintento devolvio el evento existente. */
public record SanitaryEventCreation(SanitaryEvent event, boolean created) {
}
