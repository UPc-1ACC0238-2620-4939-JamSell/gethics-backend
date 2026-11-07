package com.jamsell.gethics.notifications.domain.services;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.commands.SetNotificationPreferenceCommand;

public interface NotificationPreferenceCommandService {

    NotificationPreference handle(SetNotificationPreferenceCommand command);
}
