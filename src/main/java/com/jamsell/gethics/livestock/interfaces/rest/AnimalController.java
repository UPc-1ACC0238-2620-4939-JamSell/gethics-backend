package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterAnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.AnimalAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/animals")
public class AnimalController {

    private final AnimalCommandService commandService;

    public AnimalController(AnimalCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<AnimalResource> registerAnimal(@Valid @RequestBody RegisterAnimalResource resource) {
        var animal = commandService.handle(AnimalAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(AnimalAssembler.toResource(animal));
    }
}
