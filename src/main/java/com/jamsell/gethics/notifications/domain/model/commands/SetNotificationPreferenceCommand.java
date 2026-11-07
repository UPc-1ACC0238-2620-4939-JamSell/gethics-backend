package com.jamsell.gethics.notifications.domain.model.commands;

import java.util.UUID;

public record SetNotificationPreferenceCommand(UUID userId, boolean pushEnabled) {
}
