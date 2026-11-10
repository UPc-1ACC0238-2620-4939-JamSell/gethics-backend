package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryQueryService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ClinicalHistoryResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.ClinicalHistoryAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Limitaciones (US-14): no hay IAM ni Veterinary Care, asi que no se valida quien consulta ni si es propietario o
 * veterinario asignado del animal. Tampoco se valida que el animal exista (livestock aun no lo permite): un UUID valido
 * pero inexistente responde 200 con "Sin registros.", igual que un animal sin eventos.
 */
@Tag(name = "Sanitary")
@RestController
@RequestMapping("/api/v1/animals/{animalId}/clinical-history")
public class ClinicalHistoryController {

    private final ClinicalHistoryQueryService queryService;

    public ClinicalHistoryController(ClinicalHistoryQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Consultar el historial clinico de un animal",
            description = "US-14: todos los eventos sanitarios del animal en orden cronologico (incluye programados y cancelados). Se conserva aunque el animal haya sido dado de baja (US-08). Sin eventos devuelve 200 con un mensaje de sin registros.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial clinico (puede venir vacio)"),
        @ApiResponse(responseCode = "400", description = "animalId con formato invalido")
    })
    @GetMapping
    public ClinicalHistoryResource getClinicalHistory(@PathVariable UUID animalId) {
        return ClinicalHistoryAssembler.toResource(animalId, queryService.handle(new GetClinicalHistoryQuery(animalId)));
    }
}
