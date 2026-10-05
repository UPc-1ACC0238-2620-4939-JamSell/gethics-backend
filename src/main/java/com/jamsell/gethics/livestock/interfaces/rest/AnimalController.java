package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterAnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.UpdateAnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.AnimalAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    /** US-07: precarga los datos del animal antes de editarlo. */
    @GetMapping("/{id}")
    public ResponseEntity<AnimalResource> getAnimal(@PathVariable UUID id) {
        var animal = queryService.handle(new GetAnimalByIdQuery(id));
        return ResponseEntity.ok(AnimalAssembler.toResource(animal));
    }

    /** US-07, Escenario 1: guarda los cambios del animal. El "Cancelar" del Escenario 2 no llega a esta API. */
    @PutMapping("/{id}")
    public ResponseEntity<AnimalResource> updateAnimal(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateAnimalResource resource) {
        var animal = commandService.handle(AnimalAssembler.toCommand(id, resource));
        return ResponseEntity.ok(AnimalAssembler.toResource(animal));
    }
}
