package com.jamsell.gethics.livestock.interfaces.rest.transform;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterAnimalResource;

import java.util.List;

public final class AnimalAssembler {

    static final String NO_RESULTS = "Sin resultados.";
    static final String NO_ANIMALS = "No hay animales registrados.";

    private AnimalAssembler() {
    }

    public static RegisterAnimalCommand toCommand(RegisterAnimalResource resource) {
        return new RegisterAnimalCommand(resource.tag(), resource.name(), resource.breed(), resource.sex(),
                resource.birthDate(), resource.initialWeightKg(), resource.photoUrl(), resource.farmId());
    }

    public static AnimalResource toResource(Animal animal) {
        return new AnimalResource(animal.getId(), animal.getFarmId(), animal.getTag(), animal.getQrCode(),
                animal.getName(), animal.getBreed(), animal.getSex(), animal.getBirthDate(),
                animal.getInitialWeightKg(), animal.getPhotoUrl(), animal.getStatus());
    }

    /** Con busqueda y sin coincidencias: "Sin resultados."; sin busqueda y sin animales: "No hay animales registrados.". */
    public static AnimalListResource toListResource(GetAnimalsQuery query, List<Animal> animals) {
        var items = animals.stream().map(AnimalAssembler::toResource).toList();
        var message = !items.isEmpty() ? null : query.search() != null ? NO_RESULTS : NO_ANIMALS;
        return new AnimalListResource(items, message);
    }
}
