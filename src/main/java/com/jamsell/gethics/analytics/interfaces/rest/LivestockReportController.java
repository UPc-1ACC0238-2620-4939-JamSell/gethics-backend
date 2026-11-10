package com.jamsell.gethics.analytics.interfaces.rest;

import com.jamsell.gethics.analytics.domain.exceptions.ReportDataNotAvailableException;
import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.services.LivestockReportExportService;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import com.jamsell.gethics.analytics.interfaces.rest.resources.LivestockReportResource;
import com.jamsell.gethics.analytics.interfaces.rest.transform.LivestockReportResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-20: reportes y estadisticas del ganado por rango de fechas. US-39: exportacion del mismo reporte en CSV.
 */
@Tag(name = "Analytics")
@RestController
@RequestMapping("/api/v1/reports")
public class LivestockReportController {

    static final MediaType TEXT_CSV_UTF8 = MediaType.parseMediaType("text/csv;charset=UTF-8");

    private final LivestockReportQueryService queryService;
    private final LivestockReportExportService exportService;

    public LivestockReportController(LivestockReportQueryService queryService,
                                     LivestockReportExportService exportService) {
        this.queryService = queryService;
        this.exportService = exportService;
    }

    @Operation(summary = "Reporte y estadisticas del ganado",
            description = "US-20: indicadores de salud, productividad y finanzas del ganadero en el rango from-to (formato yyyy-MM-dd).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reporte generado"),
        @ApiResponse(responseCode = "400", description = "Rango invalido (from posterior a to) o parametros ausentes"),
        @ApiResponse(responseCode = "404", description = "No hay datos suficientes para el periodo")
    })
    @GetMapping
    public ResponseEntity<LivestockReportResource> getReport(
            @RequestParam UUID ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var report = queryService.handle(new GetLivestockReportQuery(ownerId, from, to));
        return ResponseEntity.ok(LivestockReportResourceAssembler.toResourceFromEntity(report));
    }

    /** US-39: descarga el reporte del periodo como archivo CSV. */
    @Operation(summary = "Exportar el reporte del ganado en CSV",
            description = "US-39: descarga el mismo reporte del periodo como archivo CSV (text/csv, UTF-8) con Content-Disposition attachment.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Archivo CSV del reporte"),
        @ApiResponse(responseCode = "400", description = "Rango invalido (from posterior a to) o parametros ausentes"),
        @ApiResponse(responseCode = "404", description = "No hay datos suficientes para el periodo")
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam UUID ownerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var csv = exportService.export(new GetLivestockReportQuery(ownerId, from, to));
        var fileName = "reporte-ganado-" + from + "-" + to + ".csv";
        return ResponseEntity.ok()
                .contentType(TEXT_CSV_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build()
                        .toString())
                .body(csv);
    }

    // Los errores fijan JSON explicitamente: asi tambien llegan como JSON a un cliente que pidio text/csv.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ReportDataNotAvailableException.class)
    public ResponseEntity<Map<String, String>> handleNoData(ReportDataNotAvailableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", exception.getMessage()));
    }
}
