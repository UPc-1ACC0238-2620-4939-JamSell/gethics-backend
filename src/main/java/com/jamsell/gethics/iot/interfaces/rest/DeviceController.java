package com.jamsell.gethics.iot.interfaces.rest;

import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.services.DeviceCommandService;
import com.jamsell.gethics.iot.domain.services.DeviceQueryService;
import com.jamsell.gethics.iot.interfaces.rest.resources.DeviceResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.LinkDeviceResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.RegisterSensorReadingResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.SensorReadingResource;
import com.jamsell.gethics.iot.interfaces.rest.transform.DeviceAssembler;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
    @PostMapping("/link")
    public ResponseEntity<DeviceResource> link(@Valid @RequestBody LinkDeviceResource resource) {
        var device = commandService.handle(DeviceAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceAssembler.toResource(device));
    }

    /** Dispositivos de un ganadero, con su estado actual (CONNECTED/DISCONNECTED). */
    @GetMapping
    public List<DeviceResource> listDevices(@RequestParam UUID ownerId) {
        return DeviceAssembler.toResourceList(queryService.handle(new GetDevicesByOwnerQuery(ownerId)));
    }

    /** Ingesta HTTP de una lectura del sensor para un dispositivo ya vinculado. */
    @PostMapping("/{code}/readings")
    public ResponseEntity<SensorReadingResource> registerReading(@PathVariable String code,
            @Valid @RequestBody RegisterSensorReadingResource resource) {
        var reading = commandService.handle(DeviceAssembler.toCommand(code, resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceAssembler.toResource(reading));
    }
}
