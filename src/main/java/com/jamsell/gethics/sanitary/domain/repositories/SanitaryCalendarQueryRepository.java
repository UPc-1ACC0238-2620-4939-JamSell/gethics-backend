package com.jamsell.gethics.sanitary.domain.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

import java.time.LocalDate;
import java.util.List;

/** Puerto de lectura del calendario: consulta eventos de todos los historiales, no persiste aggregates. */
public interface SanitaryCalendarQueryRepository {
    /** Eventos SCHEDULED con {@code from <= scheduledDate < toExclusive}, por fecha ascendente, con su historial ya cargado. */
    List<SanitaryEvent> findScheduledBetween(LocalDate from, LocalDate toExclusive);

    /**
     * US-46: eventos SCHEDULED con {@code scheduledDate < toExclusive}, incluidos los vencidos, por fecha ascendente, con
     * su historial ya cargado.
     */
    List<SanitaryEvent> findScheduledBefore(LocalDate toExclusive);
}
