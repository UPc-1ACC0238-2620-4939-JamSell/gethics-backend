package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;

import java.util.List;

public interface AnimalQueryService {

    /** Animales que cumplen la consulta, por arete ascendente. Lista vacia si ninguno coincide. */
    List<Animal> handle(GetAnimalsQuery query);
}
