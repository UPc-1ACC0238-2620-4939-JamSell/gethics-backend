package com.jamsell.gethics.sanitary.domain.model.valueobjects;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

import java.time.LocalDate;
import java.util.List;

/**
 * US-46: resultado de la consulta de eventos proximos. Lleva el "hoy" del Clock con el que se calculo la ventana para
 * que la proximidad (dias restantes, vencido) se mida con la misma fecha.
 */
public record UpcomingSanitaryEvents(LocalDate today, int days, List<SanitaryEvent> events) {

    public UpcomingSanitaryEvents {
        events = List.copyOf(events);
    }
}
