package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.queries.GetSanitaryCalendarQuery;
import com.jamsell.gethics.sanitary.domain.services.SanitaryCalendarQueryService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryCalendarResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.SanitaryCalendarAssembler;
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
@RequestMapping("/api/v1/sanitary-calendar")
public class SanitaryCalendarController {

    private final SanitaryCalendarQueryService queryService;

    public SanitaryCalendarController(SanitaryCalendarQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "Calendario sanitario del mes",
            description = "US-12: eventos sanitarios programados del mes indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Calendario del mes (puede venir vacio)"),
        @ApiResponse(responseCode = "400", description = "Periodo invalido (mes fuera de 1-12 o anio fuera de rango)")
    })
    @GetMapping
    public SanitaryCalendarResource getCalendar(@RequestParam int year, @RequestParam int month) {
        var query = GetSanitaryCalendarQuery.of(year, month);
        return SanitaryCalendarAssembler.toResource(query.period(), queryService.handle(query));
    }
}
