package com.jamsell.gethics.shared.application.internal.outboundservices;

import java.util.UUID;

public record PushNotificationMessage(UUID userId, String category, String title, String body) {
}
