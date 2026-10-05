package com.jamsell.gethics.notifications.domain.services;

import com.jamsell.gethics.notifications.domain.model.commands.SendPushNotificationCommand;
import com.jamsell.gethics.notifications.domain.model.valueobjects.PushNotificationDispatchResult;

public interface NotificationDispatchCommandService {

    PushNotificationDispatchResult handle(SendPushNotificationCommand command);
}
