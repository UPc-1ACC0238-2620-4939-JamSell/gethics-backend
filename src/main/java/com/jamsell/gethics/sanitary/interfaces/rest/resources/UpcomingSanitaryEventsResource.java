package com.jamsell.gethics.sanitary.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.List;

/** US-46: {@code message} solo viene cuando no hay eventos en la ventana de alerta. */
public record UpcomingSanitaryEventsResource(
        LocalDate today,
        int days,
        List<UpcomingSanitaryEventResource> events,
        String message) {
}
