package com.jamsell.gethics.notifications.application.internal.commandservices;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.commands.SetNotificationPreferenceCommand;
import com.jamsell.gethics.notifications.domain.services.NotificationPreferenceCommandService;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceCommandServiceImpl implements NotificationPreferenceCommandService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceCommandServiceImpl(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Override
    @Transactional
    public NotificationPreference handle(SetNotificationPreferenceCommand command) {
        var preference = notificationPreferenceRepository.findByUserId(command.userId())
                .orElseGet(() -> new NotificationPreference(command.userId()));
        preference.setPushEnabled(command.pushEnabled());
        return notificationPreferenceRepository.saveAndFlush(preference);
    }
}
