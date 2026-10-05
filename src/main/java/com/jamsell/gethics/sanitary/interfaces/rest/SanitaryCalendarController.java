package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.queries.GetSanitaryCalendarQuery;
import com.jamsell.gethics.sanitary.domain.services.SanitaryCalendarQueryService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryCalendarResource;
import com.jamsell.gethics.sanitary.interfaces.rest.transform.SanitaryCalendarAssembler;
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

    @GetMapping
    public SanitaryCalendarResource getCalendar(@RequestParam int year, @RequestParam int month) {
        var query = GetSanitaryCalendarQuery.of(year, month);
        return SanitaryCalendarAssembler.toResource(query.period(), queryService.handle(query));
    }
}
