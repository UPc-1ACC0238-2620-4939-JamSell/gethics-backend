package com.jamsell.gethics.livestock.domain.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;

import java.util.Optional;
import java.util.UUID;

public interface AnimalRepository {
    /** {@code tag} ya normalizado con {@link Animal#normalizeTag}. */
    boolean existsByTag(String tag);

    boolean existsById(UUID id);

    Optional<Animal> findById(UUID id);

    /** US-29: {@code qrCode} ya normalizado (mayusculas, sin espacios). */
    Optional<Animal> findByQrCode(String qrCode);

    /** Persiste el animal y devuelve la instancia persistida (con id asignado). */
    Animal save(Animal animal);
}
