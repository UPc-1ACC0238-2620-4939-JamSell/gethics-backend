package com.jamsell.gethics.analytics.interfaces.rest;

import com.jamsell.gethics.analytics.domain.model.queries.GetAlertsByOwnerQuery;
import com.jamsell.gethics.analytics.domain.services.AlertQueryService;
import com.jamsell.gethics.analytics.interfaces.rest.resources.AlertListResource;
import com.jamsell.gethics.analytics.interfaces.rest.transform.AlertResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * US-21: alertas por tendencia del ganadero, de la mas reciente a la mas antigua.
 * <p>
 * Limitacion: IAM aun no esta integrado, asi que el dueno llega como {@code ownerId} en la solicitud y no se valida que
 * corresponda a quien la envia ni que el usuario exista (un owner desconocido devuelve la lista vacia). Cuando IAM este
 * disponible, el dueno saldra del usuario autenticado y este parametro desaparecera.
 */
@Tag(name = "Analytics")
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertQueryService queryService;

    public AlertController(AlertQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Listar las alertas de tendencia del ganadero",
            description = "US-21: alertas automaticas generadas por tendencias, de la mas reciente a la mas antigua. Un ownerId sin alertas devuelve la lista vacia.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de alertas"),
        @ApiResponse(responseCode = "400", description = "ownerId ausente o con formato invalido")
    })
    @GetMapping
    public AlertListResource listAlerts(@RequestParam UUID ownerId) {
        return AlertResourceAssembler.toListResource(queryService.handle(new GetAlertsByOwnerQuery(ownerId)));
    }
}
