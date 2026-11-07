package com.jamsell.gethics.iot.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
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
}
