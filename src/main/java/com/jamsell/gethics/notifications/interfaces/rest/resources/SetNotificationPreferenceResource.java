package com.jamsell.gethics.notifications.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

public record SetNotificationPreferenceResource(@NotNull(message = "pushEnabled is required") Boolean pushEnabled) {
}
