package com.jamsell.gethics.notifications.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.commands.SetNotificationPreferenceCommand;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationPreferenceCommandServiceImplTest {

    private final NotificationPreferenceRepository repository = mock(NotificationPreferenceRepository.class);
    private final NotificationPreferenceCommandServiceImpl service = new NotificationPreferenceCommandServiceImpl(repository);

    private final UUID userId = UUID.randomUUID();

    @Test
    void createsAPreferenceWhenTheUserHasNone() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var preference = service.handle(new SetNotificationPreferenceCommand(userId, false));

        assertEquals(userId, preference.getUserId());
        assertFalse(preference.isPushEnabled());
    }

    @Test
    void updatesTheExistingPreferenceInstead() {
        var existing = new NotificationPreference(userId);
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        var preference = service.handle(new SetNotificationPreferenceCommand(userId, false));

        assertFalse(preference.isPushEnabled());
    }
}
