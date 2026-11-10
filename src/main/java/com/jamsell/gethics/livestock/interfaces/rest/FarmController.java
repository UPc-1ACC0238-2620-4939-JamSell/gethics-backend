package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.commands.UpdateFarmCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.services.FarmCommandService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.FarmListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.FarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterFarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.UpdateFarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.FarmAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Limitacion (US-09): IAM aun no esta integrado, asi que el dueno llega como {@code ownerId} en la solicitud y no se
 * valida que corresponda a quien la envia ni que el usuario exista. Cuando IAM este disponible, el dueno saldra del
 * usuario autenticado y este parametro desaparecera.
 */
@Tag(name = "Livestock", description = "Animales y granjas")
@RestController
@RequestMapping("/api/v1/farms")
public class FarmController {

    private final FarmCommandService commandService;
    private final FarmQueryService queryService;

    public FarmController(FarmCommandService commandService, FarmQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Registrar una granja",
            description = "Crea una granja para el dueno indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Granja registrada"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "409", description = "El dueno ya tiene una granja con ese nombre")
    })
    @PostMapping
    public ResponseEntity<FarmResource> registerFarm(@Valid @RequestBody RegisterFarmResource resource) {
        var farm = commandService.handle(FarmAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(FarmAssembler.toResource(farm));
    }

    /** Granjas del dueno por nombre ascendente. */
    @Operation(summary = "Listar granjas del dueno",
            description = "Devuelve las granjas del dueno ordenadas por nombre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de granjas"),
        @ApiResponse(responseCode = "400", description = "ownerId ausente o con formato invalido")
    })
    @GetMapping
    public FarmListResource listFarms(@RequestParam UUID ownerId) {
        return FarmAssembler.toListResource(queryService.handle(new GetFarmsByOwnerQuery(ownerId)));
    }

    /** US-32: reemplazo completo de nombre, ubicacion y tamano de la granja. */
    @Operation(summary = "Editar una granja",
            description = "US-32: actualiza nombre, ubicacion y tamano (tamano ausente = sin registrar). El nombre no puede repetirse entre las granjas del mismo dueno.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Granja actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (nombre o ubicacion vacios, tamano <= 0)"),
        @ApiResponse(responseCode = "404", description = "La granja no existe"),
        @ApiResponse(responseCode = "409", description = "El dueno ya tiene otra granja con ese nombre")
    })
    @PutMapping("/{id}")
    public FarmResource updateFarm(@PathVariable UUID id, @Valid @RequestBody UpdateFarmResource resource) {
        return FarmAssembler.toResource(commandService.handle(
                new UpdateFarmCommand(id, resource.name(), resource.location(), resource.sizeHectares())));
    }
}
