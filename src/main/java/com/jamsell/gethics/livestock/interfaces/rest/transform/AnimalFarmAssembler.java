package com.jamsell.gethics.livestock.interfaces.rest.transform;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.AssignAnimalToFarmCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalFarmAssignmentResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalFarmHistoryResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AssignFarmResource;

import java.util.List;
import java.util.UUID;

public final class AnimalFarmAssembler {

    static final String NO_CHANGES = "Sin cambios de granja.";
    static final String FARM_WITHOUT_ANIMALS = "Esta granja no tiene animales.";

    private AnimalFarmAssembler() {
    }

    public static AssignAnimalToFarmCommand toCommand(UUID animalId, AssignFarmResource resource) {
        return new AssignAnimalToFarmCommand(animalId, resource.farmId());
    }

    public static AnimalFarmHistoryResource toHistoryResource(UUID animalId, List<AnimalFarmAssignment> assignments) {
        var items = assignments.stream()
                .map(a -> new AnimalFarmAssignmentResource(a.getFromFarmId(), a.getToFarmId(), a.getAssignedAt()))
                .toList();
        return new AnimalFarmHistoryResource(animalId, items, items.isEmpty() ? NO_CHANGES : null);
    }

    public static AnimalListResource toFarmAnimalsResource(List<Animal> animals) {
        var items = animals.stream().map(AnimalAssembler::toResource).toList();
        return new AnimalListResource(items, items.isEmpty() ? FARM_WITHOUT_ANIMALS : null);
    }
}
