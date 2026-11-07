package com.jamsell.gethics.notifications.interfaces.rest.resources;

import java.util.UUID;

public record NotificationPreferenceResource(UUID userId, boolean pushEnabled) {
}
