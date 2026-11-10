package com.jamsell.gethics.veterinary.interfaces.rest;

import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareConflictException;
import com.jamsell.gethics.veterinary.domain.services.CareRecordCommandService;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.CareRecordResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.RegisterCareResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.SyncCareResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.SyncCareResponseResource;
import com.jamsell.gethics.veterinary.interfaces.rest.transform.CareRecordAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Veterinary")
@RestController
@RequestMapping("/api/v1/vet")
public class VeterinaryCareController {

    private final CareRecordCommandService commandService;

    public VeterinaryCareController(CareRecordCommandService commandService) {
        this.commandService = commandService;
    }

    @Operation(summary = "Registrar una atencion veterinaria",
            description = "US-19 / TS07: valida que el paciente exista, este activo y sea de un cliente asignado al veterinario; guarda la atencion, la agrega al historial clinico del animal (visible para el ganadero) y, si trae proximo control futuro, lo programa como CHECKUP en el calendario sanitario. Es idempotente por clientRequestId: 201 la primera vez y 200 en un reintento identico.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Atencion registrada"),
        @ApiResponse(responseCode = "200", description = "Reintento idempotente: devuelve la atencion ya registrada"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (diagnostico o tratamiento vacios, fecha futura, control anterior a la atencion)"),
        @ApiResponse(responseCode = "403", description = "El veterinario no esta asignado al cliente dueno del animal"),
        @ApiResponse(responseCode = "404", description = "El paciente (animal) no existe"),
        @ApiResponse(responseCode = "409", description = "El clientRequestId ya se uso con otros datos, o el animal ya no esta activo / no tiene granja"),
        @ApiResponse(responseCode = "503", description = "La atencion quedo guardada pero el historial clinico aun no se actualizo: reintentar con el mismo clientRequestId")
    })
    @PostMapping("/patients/{patientId}/care")
    public ResponseEntity<CareRecordResource> registerCare(@PathVariable UUID patientId,
                                                           @Valid @RequestBody RegisterCareResource resource) {
        var result = commandService.handle(CareRecordAssembler.toCommand(patientId, resource));
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(CareRecordAssembler.toResource(result.record()));
    }

    @Operation(summary = "Sincronizar atenciones veterinarias en lote",
            description = "US-19 / US-35: procesa hasta 100 atenciones registradas sin conexion. Cada item devuelve su resultado (CREATED, DUPLICATE, CONFLICT, REJECTED por regla de negocio, o FAILED si el historial no se pudo actualizar y hay que reintentar); un item fallido no detiene al resto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lote procesado; el estado de cada item viene en results"),
        @ApiResponse(responseCode = "400", description = "Lote vacio o con mas de 100 items")
    })
    @PostMapping("/care/sync")
    public ResponseEntity<SyncCareResponseResource> sync(@Valid @RequestBody SyncCareResource resource) {
        var items = resource.items().stream().map(CareRecordAssembler::toCommand).toList();
        var results = commandService.handle(new SyncCareBatchCommand(items)).stream()
                .map(outcome -> CareRecordAssembler.toResource(outcome))
                .toList();
        return ResponseEntity.ok(new SyncCareResponseResource(results));
    }

    @ExceptionHandler(CareConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(CareConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
