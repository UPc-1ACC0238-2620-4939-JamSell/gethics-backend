package com.jamsell.gethics.analytics.application.internal.outboundservices;

import java.util.UUID;

public record AlertNotification(UUID alertId, UUID ownerId, String message) {
}
