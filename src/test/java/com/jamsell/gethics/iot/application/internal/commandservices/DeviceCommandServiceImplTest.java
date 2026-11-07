package com.jamsell.gethics.iot.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceDisconnectionNotification;
import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceNotificationDeliveryException;
import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceNotificationService;
import com.jamsell.gethics.iot.domain.exceptions.DeviceNotFoundException;
import com.jamsell.gethics.iot.domain.exceptions.DuplicateDeviceCodeException;
import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.commands.RegisterSensorReadingCommand;
import com.jamsell.gethics.iot.domain.model.commands.SynchronizeDevicesCommand;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DeviceCommandServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2042, 6, 7, 8, 0);
    private static final Clock CLOCK = Clock.fixed(
            NOW.atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
    private static final long THRESHOLD_MINUTES = 30;

    private final DeviceRepository repository = mock(DeviceRepository.class);
    private final DeviceNotificationService notificationService = mock(DeviceNotificationService.class);
    private final DeviceCommandServiceImpl service =
            new DeviceCommandServiceImpl(repository, notificationService, CLOCK, THRESHOLD_MINUTES);

    // --- US-23 Escenario 1: vinculacion de dispositivo ---

    @Test
    void linksANewDevice() {
        when(repository.existsByCode("MX-DEV-1")).thenReturn(false);
        when(repository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        var device = service.handle(new LinkDeviceCommand(UUID.randomUUID(), "mx-dev-1"));

        assertEquals("MX-DEV-1", device.getCode());
        assertEquals(DeviceStatus.CONNECTED, device.getStatus());
        verify(repository).save(device);
    }

    @Test
    void rejectsADuplicateCodeWithoutSaving() {
        when(repository.existsByCode("MX-DEV-1")).thenReturn(true);

        assertThrows(DuplicateDeviceCodeException.class,
                () -> service.handle(new LinkDeviceCommand(UUID.randomUUID(), "mx-dev-1")));

        verify(repository, never()).save(any());
    }

    @Test
    void mapsAUniqueConstraintViolationToDuplicateCode() {
        when(repository.existsByCode("MX-DEV-1")).thenReturn(false);
        when(repository.save(any(Device.class))).thenThrow(new DataIntegrityViolationException("uk_devices_code"));

        assertThrows(DuplicateDeviceCodeException.class,
                () -> service.handle(new LinkDeviceCommand(UUID.randomUUID(), "mx-dev-1")));
    }

    // --- Ingesta de lecturas ---

    @Test
    void registersAReadingForALinkedDevice() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW.minusDays(1));
        when(repository.findByCode("MX-DEV-1")).thenReturn(Optional.of(device));
        when(repository.save(device)).thenReturn(device);

        var reading = service.handle(new RegisterSensorReadingCommand("mx-dev-1", "temperature",
                new BigDecimal("36.5"), null));

        assertEquals("temperature", reading.getMetric());
        assertEquals(NOW, device.getLastReadingAt());
        verify(repository).save(device);
    }

    @Test
    void registeringAReadingForAnUnknownCodeThrowsNotFound() {
        when(repository.findByCode("MX-DEV-1")).thenReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class, () -> service.handle(
                new RegisterSensorReadingCommand("mx-dev-1", "temperature", new BigDecimal("36.5"), null)));

        verify(repository, never()).save(any());
    }

    // --- US-23 Escenario 2: perdida de conexion ---

    @Test
    void synchronizeMarksStaleConnectedDevicesAsDisconnectedAndNotifiesTheOwner() {
        var ownerId = UUID.randomUUID();
        var staleDevice = Device.link(new LinkDeviceCommand(ownerId, "MX-DEV-1"), NOW.minusHours(2));
        when(repository.findByStatus(DeviceStatus.CONNECTED)).thenReturn(List.of(staleDevice));
        when(repository.save(staleDevice)).thenReturn(staleDevice);

        service.handle(new SynchronizeDevicesCommand());

        assertEquals(DeviceStatus.DISCONNECTED, staleDevice.getStatus());
        verify(repository).save(staleDevice);
        verify(notificationService).send(any(DeviceDisconnectionNotification.class));
    }

    @Test
    void synchronizeLeavesRecentlyActiveDevicesConnected() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW.minusMinutes(5));
        when(repository.findByStatus(DeviceStatus.CONNECTED)).thenReturn(List.of(device));

        service.handle(new SynchronizeDevicesCommand());

        assertEquals(DeviceStatus.CONNECTED, device.getStatus());
        verify(repository, never()).save(any());
        verify(notificationService, never()).send(any());
    }

    @Test
    void synchronizeContinuesWithOtherDevicesWhenANotificationFails() {
        var firstStale = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW.minusHours(2));
        var secondStale = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-2"), NOW.minusHours(2));
        when(repository.findByStatus(DeviceStatus.CONNECTED)).thenReturn(List.of(firstStale, secondStale));
        when(repository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));
        doThrowOnFirstCall();

        service.handle(new SynchronizeDevicesCommand());

        assertEquals(DeviceStatus.DISCONNECTED, firstStale.getStatus());
        assertEquals(DeviceStatus.DISCONNECTED, secondStale.getStatus());
        verify(repository, times(2)).save(any(Device.class));
        verify(notificationService, times(2)).send(any(DeviceDisconnectionNotification.class));
    }

    private void doThrowOnFirstCall() {
        doThrow(new DeviceNotificationDeliveryException("no se pudo entregar"))
                .doNothing()
                .when(notificationService).send(any(DeviceDisconnectionNotification.class));
    }
}
