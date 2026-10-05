package com.jamsell.gethics.notifications.domain.model.commands;

import com.jamsell.gethics.notifications.domain.model.valueobjects.NotificationCategory;
import java.util.UUID;

public record SendPushNotificationCommand(UUID userId, NotificationCategory category, String title, String body) {
}
