package com.jamsell.gethics.sanitary.domain.model.queries;

import java.util.Set;
import java.util.UUID;

/** Eventos COMPLETED de varios animales en una sola consulta (resumen clinico de una lista de pacientes). */
public record GetCompletedEventsByAnimalsQuery(Set<UUID> animalIds) {

    public GetCompletedEventsByAnimalsQuery {
        animalIds = animalIds == null ? Set.of() : Set.copyOf(animalIds);
    }
}
