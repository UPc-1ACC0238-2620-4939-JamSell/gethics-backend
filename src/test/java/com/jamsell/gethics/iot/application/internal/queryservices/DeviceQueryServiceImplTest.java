package com.jamsell.gethics.iot.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;

import com.jamsell.gethics.iot.domain.exceptions.DeviceNotFoundException;
import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.queries.GetDeviceReadingsQuery;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeviceQueryServiceImplTest {

    private final DeviceRepository repository = mock(DeviceRepository.class);
    private final DeviceQueryServiceImpl service = new DeviceQueryServiceImpl(repository);

    @Test
    void returnsTheDevicesOfAnOwner() {
        var ownerId = UUID.randomUUID();
        var device = Device.link(new LinkDeviceCommand(ownerId, "MX-DEV-1"), LocalDateTime.now());
        when(repository.findByOwnerId(ownerId)).thenReturn(List.of(device));

        var result = service.handle(new GetDevicesByOwnerQuery(ownerId));

        assertEquals(1, result.size());
        assertEquals("MX-DEV-1", result.get(0).getCode());
    }

    @Test
    void returnsAnEmptyListWhenTheOwnerHasNoDevices() {
        var ownerId = UUID.randomUUID();
        when(repository.findByOwnerId(ownerId)).thenReturn(List.of());

        var result = service.handle(new GetDevicesByOwnerQuery(ownerId));

        assertEquals(0, result.size());
    }

    // --- US-37 ---

    @Test
    void returnsThePageOfReadingsOfAnExistingDevice() {
        var deviceId = UUID.randomUUID();
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), LocalDateTime.now());
        var reading = device.registerReading("temperature", new BigDecimal("36.5"), null, LocalDateTime.now());
        var page = new SensorReadingPage(List.of(reading), 1, 10, 11, 2);
        when(repository.existsById(deviceId)).thenReturn(true);
        when(repository.findReadingsByDeviceId(deviceId, 1, 10)).thenReturn(page);

        var result = service.handle(new GetDeviceReadingsQuery(deviceId, 1, 10));

        assertEquals(page, result);
    }

    @Test
    void returnsAnEmptyPageWhenAnExistingDeviceHasNoReadings() {
        var deviceId = UUID.randomUUID();
        when(repository.existsById(deviceId)).thenReturn(true);
        when(repository.findReadingsByDeviceId(deviceId, 0, 20)).thenReturn(new SensorReadingPage(List.of(), 0, 20, 0, 0));

        var result = service.handle(new GetDeviceReadingsQuery(deviceId, 0, 20));

        assertEquals(0, result.content().size());
        assertEquals(0, result.totalElements());
    }

    @Test
    void throwsNotFoundWhenTheDeviceDoesNotExist() {
        var deviceId = UUID.randomUUID();
        when(repository.existsById(deviceId)).thenReturn(false);

        assertThrows(DeviceNotFoundException.class, () -> service.handle(new GetDeviceReadingsQuery(deviceId, 0, 20)));

        verify(repository, never()).findReadingsByDeviceId(any(), anyInt(), anyInt());
    }
}
