package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.AssignAnimalToFarmCommand;

public interface AnimalFarmCommandService {

    /**
     * Asigna el animal a la granja o lo mueve de una a otra, registrando el cambio en el historial. Si el animal ya
     * esta en esa granja no cambia nada ni se registra otro cambio.
     */
    Animal handle(AssignAnimalToFarmCommand command);
}
