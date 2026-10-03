package com.jamsell.gethics.sanitary.domain.repositories;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;

import java.util.Optional;
import java.util.UUID;

public interface ClinicalHistoryRepository {
    Optional<ClinicalHistory> findByAnimalId(UUID animalId);

    /** Persiste el aggregate (incluidos sus eventos) y devuelve la instancia persistida. */
    ClinicalHistory save(ClinicalHistory clinicalHistory);
}
