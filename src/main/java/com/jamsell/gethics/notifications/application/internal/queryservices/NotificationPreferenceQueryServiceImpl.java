package com.jamsell.gethics.notifications.application.internal.queryservices;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.queries.GetNotificationPreferenceQuery;
import com.jamsell.gethics.notifications.domain.services.NotificationPreferenceQueryService;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceQueryServiceImpl implements NotificationPreferenceQueryService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceQueryServiceImpl(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean handle(GetNotificationPreferenceQuery query) {
        return notificationPreferenceRepository.findByUserId(query.userId())
                .map(NotificationPreference::isPushEnabled)
                .orElse(true);
    }
}
