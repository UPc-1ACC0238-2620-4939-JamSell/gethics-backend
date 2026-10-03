package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryQueryService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ClinicalHistoryResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.ClinicalHistoryAssembler;
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
@RestController
@RequestMapping("/api/v1/animals/{animalId}/clinical-history")
public class ClinicalHistoryController {

    private final ClinicalHistoryQueryService queryService;

    public ClinicalHistoryController(ClinicalHistoryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ClinicalHistoryResource getClinicalHistory(@PathVariable UUID animalId) {
        return ClinicalHistoryAssembler.toResource(animalId, queryService.handle(new GetClinicalHistoryQuery(animalId)));
    }
}
