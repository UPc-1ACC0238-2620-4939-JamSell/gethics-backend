package com.jamsell.gethics.livestock.domain.repositories;

import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;

import java.util.List;
import java.util.UUID;

public interface AnimalFarmAssignmentRepository {

    AnimalFarmAssignment save(AnimalFarmAssignment assignment);

    /** Historial del animal, del cambio mas antiguo al mas reciente. Lista vacia si nunca tuvo granja. */
    List<AnimalFarmAssignment> findByAnimalId(UUID animalId);
}
