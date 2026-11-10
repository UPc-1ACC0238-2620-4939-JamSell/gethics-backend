package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.commands.CancelScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventCreation;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.CompleteSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.RegisterSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ScheduleSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.SanitaryEventAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Registrar un evento sanitario",
            description = "US-11: registra un evento ya ocurrido (vacuna, tratamiento, enfermedad, chequeo u otro). Con clientRequestId es idempotente (US-35): el primer envio responde 201 y un reintento identico 200.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento registrado"),
        @ApiResponse(responseCode = "200", description = "Reintento idempotente: devuelve el evento ya registrado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (tipo o fecha ausentes, fecha futura)"),
        @ApiResponse(responseCode = "409", description = "El clientRequestId ya se uso con otros datos")
    })
    @PostMapping
    public ResponseEntity<SanitaryEventResource> registerSanitaryEvent(
            @PathVariable UUID animalId,
            @Valid @RequestBody RegisterSanitaryEventResource resource) {
        return created(commandService.handle(SanitaryEventAssembler.toCommand(animalId, resource)));
    }

    @Operation(summary = "Programar un evento sanitario",
            description = "US-12/US-13: programa un evento futuro; genera recordatorios de vacunacion. Con clientRequestId es idempotente (US-35).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento programado"),
        @ApiResponse(responseCode = "200", description = "Reintento idempotente: devuelve el evento ya programado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos o fecha programada ya vencida"),
        @ApiResponse(responseCode = "409", description = "El clientRequestId ya se uso con otros datos")
    })
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
    @Operation(summary = "Completar un evento programado",
            description = "Registra como aplicado el mismo evento programado (SCHEDULED a COMPLETED) sin crear uno nuevo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento completado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos o fecha futura"),
        @ApiResponse(responseCode = "404", description = "El animal no tiene ese evento"),
        @ApiResponse(responseCode = "409", description = "El evento no esta programado (ya completado o cancelado)")
    })
    @PostMapping("/{eventId}/complete")
    public SanitaryEventResource completeScheduledEvent(
            @PathVariable UUID animalId,
            @PathVariable UUID eventId,
            @Valid @RequestBody CompleteSanitaryEventResource resource) {
        var event = commandService.handle(SanitaryEventAssembler.toCommand(animalId, eventId, resource));
        return SanitaryEventAssembler.toResource(event);
    }

    /** US-33: cancela el evento programado (SCHEDULED -> CANCELLED) sin borrarlo. Sin body: la accion no lleva datos. */
    @Operation(summary = "Cancelar un evento programado",
            description = "US-33: cambia el evento de SCHEDULED a CANCELLED sin borrarlo; queda en el historial clinico y sale del calendario y de los recordatorios.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento cancelado"),
        @ApiResponse(responseCode = "404", description = "El animal no tiene ese evento"),
        @ApiResponse(responseCode = "409", description = "El evento no esta programado (ya completado o cancelado)")
    })
    @PostMapping("/{eventId}/cancel")
    public SanitaryEventResource cancelScheduledEvent(@PathVariable UUID animalId, @PathVariable UUID eventId) {
        var event = commandService.handle(new CancelScheduledEventCommand(animalId, eventId));
        return SanitaryEventAssembler.toResource(event);
    }
}
