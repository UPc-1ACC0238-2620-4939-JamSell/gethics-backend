package com.jamsell.gethics.livestock.domain.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;

import java.util.List;
import java.util.UUID;

/** Puerto de lectura de granjas. */
public interface FarmQueryRepository {

    /** Granjas del dueno por nombre ascendente. Lista vacia si no tiene ninguna. */
    List<Farm> findByOwnerId(UUID ownerId);
}
