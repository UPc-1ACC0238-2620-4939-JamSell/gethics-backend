package com.jamsell.gethics.livestock.domain.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;

import java.util.Optional;
import java.util.UUID;

public interface FarmRepository {
    /** {@code normalizedName} ya normalizado con {@link Farm#normalizeName}. */
    boolean existsByOwnerIdAndNormalizedName(UUID ownerId, String normalizedName);

    /** US-32: igual que {@link #existsByOwnerIdAndNormalizedName} pero ignorando la propia granja. */
    boolean existsByOwnerIdAndNormalizedNameAndIdNot(UUID ownerId, String normalizedName, UUID id);

    boolean existsById(UUID id);

    Optional<Farm> findById(UUID id);

    /** Persiste la granja y devuelve la instancia persistida (con id asignado). */
    Farm save(Farm farm);
}
