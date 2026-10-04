package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.CompleteSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.RegisterSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ScheduleSanitaryEventResource;
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

    @PostMapping("/scheduled")
    public ResponseEntity<SanitaryEventResource> scheduleSanitaryEvent(
            @PathVariable UUID animalId,
            @Valid @RequestBody ScheduleSanitaryEventResource resource) {
        var event = commandService.handle(SanitaryEventAssembler.toCommand(animalId, resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(SanitaryEventAssembler.toResource(event));
    }

    /** Registra como aplicado el MISMO evento programado (SCHEDULED -> COMPLETED); no crea un evento nuevo. */
    @PostMapping("/{eventId}/complete")
    public SanitaryEventResource completeScheduledEvent(
            @PathVariable UUID animalId,
            @PathVariable UUID eventId,
            @Valid @RequestBody CompleteSanitaryEventResource resource) {
        var event = commandService.handle(SanitaryEventAssembler.toCommand(animalId, eventId, resource));
        return SanitaryEventAssembler.toResource(event);
    }
}
