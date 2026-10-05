package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterAnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.AnimalAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Limitaciones: no hay IAM ni Farm, asi que el inventario es unico para todos los usuarios (no se filtra por dueno
 * ni por finca) y no hay paginacion (no la exige US-06).
 */
@Tag(name = "Livestock")
@RestController
@RequestMapping("/api/v1/animals")
public class AnimalController {

    private final AnimalCommandService commandService;
    private final AnimalQueryService queryService;

    public AnimalController(AnimalCommandService commandService, AnimalQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<AnimalResource> registerAnimal(@Valid @RequestBody RegisterAnimalResource resource) {
        var animal = commandService.handle(AnimalAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(AnimalAssembler.toResource(animal));
    }

    /** US-06. {@code search} busca por arete, nombre o raza; {@code status} por defecto es ACTIVE. */
    @GetMapping
    public AnimalListResource listAnimals(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) AnimalStatus status) {
        var query = GetAnimalsQuery.of(search, status);
        return AnimalAssembler.toListResource(query, queryService.handle(query));
    }
}
