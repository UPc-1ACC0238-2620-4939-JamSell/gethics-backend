package com.jamsell.gethics.analytics.interfaces.rest;

import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import com.jamsell.gethics.analytics.interfaces.rest.resources.LivestockReportResource;
import com.jamsell.gethics.analytics.interfaces.rest.transform.LivestockReportResourceAssembler;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-20: reportes y estadisticas del ganado por rango de fechas.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class LivestockReportController {

    private final LivestockReportQueryService queryService;

    public LivestockReportController(LivestockReportQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<LivestockReportResource> getReport(
            @RequestParam UUID ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var report = queryService.handle(new GetLivestockReportQuery(ownerId, from, to));
        return ResponseEntity.ok(LivestockReportResourceAssembler.toResourceFromEntity(report));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
