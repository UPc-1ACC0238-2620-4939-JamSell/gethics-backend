package com.jamsell.gethics.livestock.domain.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;

import java.util.UUID;

public interface FarmRepository {
    /** {@code normalizedName} ya normalizado con {@link Farm#normalizeName}. */
    boolean existsByOwnerIdAndNormalizedName(UUID ownerId, String normalizedName);

    boolean existsById(UUID id);

    /** Persiste la granja y devuelve la instancia persistida (con id asignado). */
    Farm save(Farm farm);
}
