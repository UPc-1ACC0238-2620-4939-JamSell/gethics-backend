package com.jamsell.gethics.iot.interfaces.rest.resources;

import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceResource(
        UUID id,
        UUID ownerId,
        String code,
        DeviceStatus status,
        LocalDateTime linkedAt,
        LocalDateTime lastReadingAt) {
}
