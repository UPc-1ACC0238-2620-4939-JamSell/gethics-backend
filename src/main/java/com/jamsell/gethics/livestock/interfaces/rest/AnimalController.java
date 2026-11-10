package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.commands.DeactivateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.AnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterAnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.UpdateAnimalResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.AnimalAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Limitaciones: no hay IAM ni Farm, asi que el inventario es unico para todos los usuarios (no se filtra por dueno
 * ni por finca) y no hay paginacion (no la exige US-06).
 */
@Tag(name = "Livestock", description = "Animales y granjas")
@RestController
@RequestMapping("/api/v1/animals")
public class AnimalController {

    private final AnimalCommandService commandService;
    private final AnimalQueryService queryService;

    public AnimalController(AnimalCommandService commandService, AnimalQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Registrar un animal",
            description = "Crea un animal ACTIVE con arete unico y genera su codigo QR.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Animal registrado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (raza vacia, peso <= 0, fecha futura)"),
        @ApiResponse(responseCode = "404", description = "La granja indicada no existe"),
        @ApiResponse(responseCode = "409", description = "Ya existe un animal con ese arete")
    })
    @PostMapping
    public ResponseEntity<AnimalResource> registerAnimal(@Valid @RequestBody RegisterAnimalResource resource) {
        var animal = commandService.handle(AnimalAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(AnimalAssembler.toResource(animal));
    }

    /** US-06. {@code search} busca por arete, nombre o raza; {@code status} por defecto es ACTIVE. */
    @Operation(summary = "Listar y buscar animales",
            description = "Busca por arete, nombre o raza. Por defecto solo devuelve animales ACTIVE; usa status para ver otros estados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de animales (puede venir vacio con un mensaje)"),
        @ApiResponse(responseCode = "400", description = "Parametro invalido (status desconocido o busqueda de mas de 100 caracteres)")
    })
    @GetMapping
    public AnimalListResource listAnimals(@RequestParam(required = false) String search,
                                          @RequestParam(required = false) AnimalStatus status) {
        var query = GetAnimalsQuery.of(search, status);
        return AnimalAssembler.toListResource(query, queryService.handle(query));
    }

    /** US-07: precarga los datos del animal antes de editarlo. */
    @Operation(summary = "Obtener un animal",
            description = "Devuelve el animal sin importar su estado, para precargar el detalle o la edicion.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Animal encontrado"),
        @ApiResponse(responseCode = "404", description = "El animal no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AnimalResource> getAnimal(@PathVariable UUID id) {
        var animal = queryService.handle(new GetAnimalByIdQuery(id));
        return ResponseEntity.ok(AnimalAssembler.toResource(animal));
    }

    /** US-07, Escenario 1: guarda los cambios del animal. El "Cancelar" del Escenario 2 no llega a esta API. */
    @Operation(summary = "Editar un animal",
            description = "Actualiza nombre, raza, sexo, fecha de nacimiento, peso, foto y, opcionalmente, la granja. No cambia el arete ni el QR.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Animal actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "404", description = "El animal o la granja no existen")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AnimalResource> updateAnimal(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateAnimalResource resource) {
        var animal = commandService.handle(AnimalAssembler.toCommand(id, resource));
        return ResponseEntity.ok(AnimalAssembler.toResource(animal));
    }

    /**
     * US-08: baja logica. El animal pasa a INACTIVE: deja de aparecer en los listados activos, pero su registro y su
     * historial sanitario y de granjas se conservan.
     */
    @Operation(summary = "Dar de baja un animal (baja logica)",
            description = "Cambia el estado a INACTIVE sin borrar el registro ni su historial sanitario. Responde 409 si el animal ya no esta ACTIVE.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Animal dado de baja"),
        @ApiResponse(responseCode = "404", description = "El animal no existe"),
        @ApiResponse(responseCode = "409", description = "El animal ya esta dado de baja")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAnimal(@PathVariable UUID id) {
        commandService.handle(new DeactivateAnimalCommand(id));
        return ResponseEntity.noContent().build();
    }
}
