package com.jamsell.gethics.iot.interfaces.rest;

import com.jamsell.gethics.iot.domain.model.queries.GetDeviceReadingsQuery;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.services.DeviceCommandService;
import com.jamsell.gethics.iot.domain.services.DeviceQueryService;
import com.jamsell.gethics.iot.interfaces.rest.resources.DeviceResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.LinkDeviceResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.RegisterSensorReadingResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.SensorReadingPageResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.SensorReadingResource;
import com.jamsell.gethics.iot.interfaces.rest.transform.DeviceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-23: vinculacion de dispositivos IoT (Escenario 1), ingesta de lecturas, y consulta de estado
 * conectado/desconectado (Escenario 2; la transicion a DISCONNECTED la aplica {@code DeviceSyncJob}).
 */
@Tag(name = "IoT")
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceCommandService commandService;
    private final DeviceQueryService queryService;

    public DeviceController(DeviceCommandService commandService, DeviceQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /** US-23 Escenario 1: el ganadero vincula su dispositivo IoT compatible mediante su codigo. */
    @Operation(summary = "Vincular un dispositivo IoT",
            description = "US-23: vincula un dispositivo compatible a un ganadero mediante su codigo unico.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dispositivo vinculado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (codigo o ownerId vacios)"),
        @ApiResponse(responseCode = "409", description = "Ya existe un dispositivo con ese codigo")
    })
    @PostMapping("/link")
    public ResponseEntity<DeviceResource> link(@Valid @RequestBody LinkDeviceResource resource) {
        var device = commandService.handle(DeviceAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceAssembler.toResource(device));
    }

    /** Dispositivos de un ganadero, con su estado actual (CONNECTED/DISCONNECTED). */
    @Operation(summary = "Listar los dispositivos de un ganadero",
            description = "Devuelve los dispositivos del ganadero con su estado actual (CONNECTED o DISCONNECTED).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de dispositivos (vacio si no tiene ninguno)"),
        @ApiResponse(responseCode = "400", description = "ownerId ausente o con formato invalido")
    })
    @GetMapping
    public List<DeviceResource> listDevices(@RequestParam UUID ownerId) {
        return DeviceAssembler.toResourceList(queryService.handle(new GetDevicesByOwnerQuery(ownerId)));
    }

    /** Ingesta HTTP de una lectura del sensor para un dispositivo ya vinculado. */
    @Operation(summary = "Registrar una lectura del sensor",
            description = "Ingesta HTTP de una lectura (metrica y valor) para un dispositivo ya vinculado, identificado por su codigo.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lectura registrada"),
        @ApiResponse(responseCode = "400", description = "Lectura invalida (metrica o valor vacios)"),
        @ApiResponse(responseCode = "404", description = "No existe un dispositivo con ese codigo")
    })
    @PostMapping("/{code}/readings")
    public ResponseEntity<SensorReadingResource> registerReading(@PathVariable String code,
            @Valid @RequestBody RegisterSensorReadingResource resource) {
        var reading = commandService.handle(DeviceAssembler.toCommand(code, resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceAssembler.toResource(reading));
    }

    /**
     * US-37: lecturas de un dispositivo, de la mas reciente a la mas antigua. Sin lecturas devuelve 200 con
     * {@code content} vacio (Escenario 2); si el dispositivo no existe, 404.
     */
    @Operation(summary = "Consultar las lecturas de un dispositivo",
            description = "US-37: lecturas paginadas, de la mas reciente a la mas antigua. Sin lecturas devuelve 200 con content vacio. "
                    + "US-48: los parametros opcionales from y to (yyyy-MM-dd) filtran por dia de calendario de recordedAt; "
                    + "ambos limites son inclusivos (from desde las 00:00:00 de ese dia, to hasta las 23:59:59.999999999 de ese dia), "
                    + "from=to devuelve un solo dia y cada uno puede omitirse (rango abierto). Sin fechas el comportamiento es el de US-37. "
                    + "recordedAt no lleva zona horaria: el dia se compara tal cual, sin conversion. "
                    + "Ejemplos: /api/v1/devices/{id}/readings?from=2042-06-01&to=2042-06-07 | ?from=2042-06-01 | ?to=2042-06-07.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagina de lecturas (content vacio si no hay lecturas en el rango)"),
        @ApiResponse(responseCode = "400", description = "page negativo, size fuera de rango, from/to con formato distinto de yyyy-MM-dd, from posterior a to o id que no es UUID"),
        @ApiResponse(responseCode = "404", description = "El dispositivo no existe")
    })
    @GetMapping("/{id}/readings")
    public SensorReadingPageResource listReadings(@PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Dia inicial inclusivo, formato yyyy-MM-dd. Opcional.", example = "2042-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Dia final inclusivo, formato yyyy-MM-dd. Opcional.", example = "2042-06-07")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return DeviceAssembler.toResource(queryService.handle(new GetDeviceReadingsQuery(id, page, size, from, to)));
    }
}
