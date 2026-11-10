package com.jamsell.gethics.sanitary.domain.repositories;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalHistoryRepository {
    Optional<ClinicalHistory> findByAnimalId(UUID animalId);

    /** Persiste el aggregate (incluidos sus eventos) y devuelve la instancia persistida. */
    ClinicalHistory save(ClinicalHistory clinicalHistory);

    /**
     * US-35: evento (de cualquier animal) creado con ese id de idempotencia, con su historial cargado. Solo lectura: decide
     * si un reintento es el mismo request; los cambios siguen entrando por el aggregate.
     */
    Optional<SanitaryEvent> findEventByClientRequestId(UUID clientRequestId);
}
