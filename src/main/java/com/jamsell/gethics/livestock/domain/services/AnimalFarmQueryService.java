package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalFarmHistoryQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;

import java.util.List;

public interface AnimalFarmQueryService {

    /** Historial de granjas del animal, del mas antiguo al mas reciente. Falla si el animal no existe. */
    List<AnimalFarmAssignment> handle(GetAnimalFarmHistoryQuery query);

    /** Animales de la granja por arete ascendente. Falla si la granja no existe. */
    List<Animal> handle(GetAnimalsByFarmQuery query);
}
