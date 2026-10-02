package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.RegisterSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.SanitaryEventAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animals/{animalId}/sanitary-events")
public class SanitaryEventController {

    private final ClinicalHistoryCommandService commandService;

    public SanitaryEventController(ClinicalHistoryCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    public ResponseEntity<SanitaryEventResource> registerSanitaryEvent(
            @PathVariable UUID animalId,
            @Valid @RequestBody RegisterSanitaryEventResource resource) {
        var event = commandService.handle(SanitaryEventAssembler.toCommand(animalId, resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(SanitaryEventAssembler.toResource(event));
    }
}
