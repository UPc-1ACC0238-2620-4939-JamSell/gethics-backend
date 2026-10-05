package com.jamsell.gethics.livestock.interfaces.rest.transform;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterAnimalResource;

public final class AnimalAssembler {

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
}
