package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalFarmHistoryQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmQueryService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalFarmHistoryResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AssignFarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.AnimalAssembler;
import com.jamsell.gethics.livestock.interfaces.rest.transform.AnimalFarmAssembler;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Asociacion animal-granja (US-10). Limitacion: el animal no tiene dueno (IAM aun no esta integrado), asi que no se
 * valida que la granja sea del mismo ganadero que el animal.
 */
@RestController
@RequestMapping("/api/v1")
public class AnimalFarmController {

    private final AnimalFarmCommandService commandService;
    private final AnimalFarmQueryService queryService;

    public AnimalFarmController(AnimalFarmCommandService commandService, AnimalFarmQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /** Asigna el animal a la granja o lo mueve a otra. Idempotente: repetir la misma granja no cambia nada. */
    @PutMapping("/animals/{animalId}/farm")
    public AnimalResource assignFarm(@PathVariable UUID animalId, @Valid @RequestBody AssignFarmResource resource) {
        return AnimalAssembler.toResource(commandService.handle(AnimalFarmAssembler.toCommand(animalId, resource)));
    }

    /** Historial de granjas del animal, del cambio mas antiguo al mas reciente. */
    @GetMapping("/animals/{animalId}/farm-history")
    public AnimalFarmHistoryResource getFarmHistory(@PathVariable UUID animalId) {
        return AnimalFarmAssembler.toHistoryResource(animalId,
                queryService.handle(new GetAnimalFarmHistoryQuery(animalId)));
    }

    /** Animales de la granja; {@code status} por defecto es ACTIVE. */
    @GetMapping("/farms/{farmId}/animals")
    public AnimalListResource listFarmAnimals(@PathVariable UUID farmId,
                                              @RequestParam(required = false) AnimalStatus status) {
        return AnimalFarmAssembler.toFarmAnimalsResource(queryService.handle(GetAnimalsByFarmQuery.of(farmId, status)));
    }
}
