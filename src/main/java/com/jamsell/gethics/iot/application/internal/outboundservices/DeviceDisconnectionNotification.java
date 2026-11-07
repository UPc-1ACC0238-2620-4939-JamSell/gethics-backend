package com.jamsell.gethics.iot.application.internal.outboundservices;

import java.util.UUID;

public record DeviceDisconnectionNotification(UUID deviceId, UUID ownerId, String code, String message) {
}
