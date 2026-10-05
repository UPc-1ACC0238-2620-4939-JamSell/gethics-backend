package com.jamsell.gethics.iot.application.internal.commandservices;

import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceDisconnectionNotification;
import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceNotificationDeliveryException;
import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceNotificationService;
import com.jamsell.gethics.iot.domain.exceptions.DeviceNotFoundException;
import com.jamsell.gethics.iot.domain.exceptions.DuplicateDeviceCodeException;
import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.commands.RegisterSensorReadingCommand;
import com.jamsell.gethics.iot.domain.model.commands.SynchronizeDevicesCommand;
import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import com.jamsell.gethics.iot.domain.services.DeviceCommandService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El umbral de desconexion (US-23 Escenario 2) es una decision de ingenieria, no un requerimiento especifico del
 * negocio: el informe no define cuanto tiempo sin lecturas cuenta como "perdida de conexion". Por defecto, 30
 * minutos; ajustable con {@code gethics.iot.offline-threshold-minutes}.
 */
@Slf4j
@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceRepository repository;
    private final DeviceNotificationService notificationService;
    private final Clock clock;
    private final Duration offlineThreshold;

    public DeviceCommandServiceImpl(DeviceRepository repository, DeviceNotificationService notificationService,
                                    Clock clock,
                                    @Value("${gethics.iot.offline-threshold-minutes:30}") long offlineThresholdMinutes) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.offlineThreshold = Duration.ofMinutes(offlineThresholdMinutes);
    }

    @Override
    @Transactional
    public Device handle(LinkDeviceCommand command) {
        var device = Device.link(command, LocalDateTime.now(clock));
        if (repository.existsByCode(device.getCode())) {
            throw new DuplicateDeviceCodeException(device.getCode());
        }
        try {
            return repository.save(device);
        } catch (DataIntegrityViolationException e) {
            // Dos vinculaciones simultaneas con el mismo codigo: gana la restriccion unica de la tabla.
            throw new DuplicateDeviceCodeException(device.getCode());
        }
    }

    @Override
    @Transactional
    public SensorReading handle(RegisterSensorReadingCommand command) {
        var device = repository.findByCode(Device.normalizeCode(command.code()))
                .orElseThrow(() -> new DeviceNotFoundException(command.code()));
        var reading = device.registerReading(command.metric(), command.value(), command.recordedAt(),
                LocalDateTime.now(clock));
        repository.save(device);
        return reading;
    }

    /**
     * Deliberadamente NO es {@code @Transactional}: cada guardado confirma por separado (igual que
     * {@code AlertDispatchCommandServiceImpl}), y la notificacion de un dispositivo que falla no detiene el resto.
     */
    @Override
    public void handle(SynchronizeDevicesCommand command) {
        var now = LocalDateTime.now(clock);
        int disconnected = 0;
        for (var device : repository.findByStatus(DeviceStatus.CONNECTED)) {
            if (!device.isStale(now, offlineThreshold)) {
                continue;
            }
            device.markDisconnected();
            repository.save(device);
            disconnected++;
            notify(device);
        }
        log.info("Sincronizacion IoT: {} dispositivos marcados como desconectados", disconnected);
    }

    private void notify(Device device) {
        try {
            notificationService.send(new DeviceDisconnectionNotification(device.getId(), device.getOwnerId(),
                    device.getCode(), "El dispositivo " + device.getCode() + " perdio conexion."));
        } catch (DeviceNotificationDeliveryException e) {
            log.warn("No se pudo notificar la desconexion del dispositivo {}: {}", device.getCode(), e.getMessage());
        }
    }
}
