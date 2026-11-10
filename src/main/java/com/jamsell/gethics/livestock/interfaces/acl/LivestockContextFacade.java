package com.jamsell.gethics.livestock.interfaces.acl;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmByIdQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmQueryService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TS07: fachada de Livestock para otros bounded contexts (hoy, Veterinary). Expone solo lecturas y solo records; el
 * "cliente" del veterinario es el ganadero dueno de las granjas (Farm.ownerId).
 */
@Service
public class LivestockContextFacade {

    private final FarmQueryService farmQueryService;
    private final AnimalFarmQueryService animalFarmQueryService;
    private final AnimalQueryService animalQueryService;

    public LivestockContextFacade(FarmQueryService farmQueryService, AnimalFarmQueryService animalFarmQueryService,
                                  AnimalQueryService animalQueryService) {
        this.farmQueryService = farmQueryService;
        this.animalFarmQueryService = animalFarmQueryService;
        this.animalQueryService = animalQueryService;
    }

    /**
     * Animales ACTIVE de todas las granjas del ganadero. Los dados de baja (US-08) no aparecen: siguen en su
     * registro, pero ya no son pacientes activos.
     */
    public List<AnimalSummary> findActiveAnimalsByOwnerId(UUID ownerId) {
        return farmQueryService.handle(new GetFarmsByOwnerQuery(ownerId)).stream()
                .flatMap(farm -> animalFarmQueryService
                        .handle(GetAnimalsByFarmQuery.of(farm.getId(), AnimalStatus.ACTIVE)).stream())
                .map(LivestockContextFacade::toSummary)
                .toList();
    }

    /** Ubicacion de la primera granja del ganadero (por nombre); vacio si no tiene granjas. */
    public Optional<String> findLocationByOwnerId(UUID ownerId) {
        return farmQueryService.handle(new GetFarmsByOwnerQuery(ownerId)).stream()
                .findFirst()
                .map(Farm::getLocation);
    }

    /**
     * Estado y dueno del animal (el dueno es el de su granja; null si no tiene granja). Vacio si el animal no existe.
     */
    public Optional<AnimalAccessInfo> findAnimalAccess(UUID animalId) {
        Animal animal;
        try {
            animal = animalQueryService.handle(new GetAnimalByIdQuery(animalId));
        } catch (AnimalNotFoundException e) {
            return Optional.empty();
        }
        UUID ownerId = animal.getFarmId() == null ? null
                : farmQueryService.handle(new GetFarmByIdQuery(animal.getFarmId())).map(Farm::getOwnerId).orElse(null);
        return Optional.of(new AnimalAccessInfo(animal.getId(), animal.getStatus() == AnimalStatus.ACTIVE, ownerId));
    }

    private static AnimalSummary toSummary(Animal animal) {
        return new AnimalSummary(animal.getId(), animal.getName(), animal.getTag(), animal.getBreed(),
                animal.getStatus().name());
    }
}
