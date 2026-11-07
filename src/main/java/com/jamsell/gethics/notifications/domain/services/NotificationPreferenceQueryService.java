package com.jamsell.gethics.notifications.domain.services;

import com.jamsell.gethics.notifications.domain.model.queries.GetNotificationPreferenceQuery;

public interface NotificationPreferenceQueryService {

    /** @return {@code true} si el usuario no tiene una preferencia guardada (las notificaciones estan activas por defecto). */
    boolean handle(GetNotificationPreferenceQuery query);
}
