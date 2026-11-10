package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.commands.CancelScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventCreation;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.CompleteSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.RegisterSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ScheduleSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.SanitaryEventAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Sanitary")
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
        return created(commandService.handle(SanitaryEventAssembler.toCommand(animalId, resource)));
    }

    @PostMapping("/scheduled")
    public ResponseEntity<SanitaryEventResource> scheduleSanitaryEvent(
            @PathVariable UUID animalId,
            @Valid @RequestBody ScheduleSanitaryEventResource resource) {
        return created(commandService.handle(SanitaryEventAssembler.toCommand(animalId, resource)));
    }

    /** US-35 (como US-19): 201 si se creo el evento; 200 si un reintento con el mismo clientRequestId devolvio el existente. */
    private static ResponseEntity<SanitaryEventResource> created(SanitaryEventCreation creation) {
        var status = creation.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(SanitaryEventAssembler.toResource(creation.event()));
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

    /** US-33: cancela el evento programado (SCHEDULED -> CANCELLED) sin borrarlo. Sin body: la accion no lleva datos. */
    @PostMapping("/{eventId}/cancel")
    public SanitaryEventResource cancelScheduledEvent(@PathVariable UUID animalId, @PathVariable UUID eventId) {
        var event = commandService.handle(new CancelScheduledEventCommand(animalId, eventId));
        return SanitaryEventAssembler.toResource(event);
    }
}
