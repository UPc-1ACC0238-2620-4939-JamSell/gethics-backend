package com.jamsell.gethics.notifications.interfaces.rest.resources;

import com.jamsell.gethics.notifications.domain.model.valueobjects.NotificationCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendPushNotificationResource(
        @NotNull(message = "User id is required") UUID userId,
        @NotNull(message = "Category is required") NotificationCategory category,
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Body is required") String body) {
}
