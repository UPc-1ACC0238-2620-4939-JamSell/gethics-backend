package com.jamsell.gethics.livestock.domain.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

import java.util.List;
import java.util.UUID;

/** Puerto de lectura del inventario. */
public interface AnimalQueryRepository {

    /**
     * Animales con ese estado cuyo arete, nombre o raza contienen {@code search} (sin distinguir mayusculas; los
     * comodines % y _ se toman literalmente). {@code search} null o vacio no filtra. Orden: arete ascendente.
     */
    List<Animal> search(String search, AnimalStatus status);

    /** Animales de esa granja con ese estado, por arete ascendente. */
    List<Animal> findByFarmId(UUID farmId, AnimalStatus status);
}
