package com.jamsell.gethics.notifications.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.queries.GetNotificationPreferenceQuery;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationPreferenceQueryServiceImplTest {

    private final NotificationPreferenceRepository repository = mock(NotificationPreferenceRepository.class);
    private final NotificationPreferenceQueryServiceImpl service = new NotificationPreferenceQueryServiceImpl(repository);

    private final UUID userId = UUID.randomUUID();

    @Test
    void userWithoutAStoredPreferenceDefaultsToEnabled() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertTrue(service.handle(new GetNotificationPreferenceQuery(userId)));
    }

    @Test
    void returnsTheStoredPreferenceWhenDisabled() {
        var preference = new NotificationPreference(userId);
        preference.setPushEnabled(false);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(preference));

        assertFalse(service.handle(new GetNotificationPreferenceQuery(userId)));
    }
}
