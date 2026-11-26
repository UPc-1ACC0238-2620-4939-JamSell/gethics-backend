package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.queries.GetUpcomingSanitaryEventsQuery;
import com.jamsell.gethics.sanitary.domain.services.UpcomingSanitaryEventsQueryService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.UpcomingSanitaryEventsResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.UpcomingSanitaryEventsAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sanitary")
@RestController
@RequestMapping("/api/v1/sanitary-events")
public class UpcomingSanitaryEventsController {

    private final UpcomingSanitaryEventsQueryService queryService;

    public UpcomingSanitaryEventsController(UpcomingSanitaryEventsQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Alertas de eventos sanitarios proximos",
            description = "US-46: eventos programados que vencen entre hoy y los proximos dias indicados, mas los ya vencidos "
                    + "que siguen pendientes, ordenados por fecha. Cada evento indica los dias que faltan y si esta vencido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Eventos proximos (la lista puede venir vacia con un mensaje)"),
        @ApiResponse(responseCode = "400", description = "Ventana de alerta fuera de rango (1 a 30 dias)")
    })
    @GetMapping("/upcoming")
    public UpcomingSanitaryEventsResource getUpcomingEvents(
            @RequestParam(defaultValue = "" + GetUpcomingSanitaryEventsQuery.DEFAULT_DAYS) int days) {
        return UpcomingSanitaryEventsAssembler.toResource(queryService.handle(new GetUpcomingSanitaryEventsQuery(days)));
    }
}
