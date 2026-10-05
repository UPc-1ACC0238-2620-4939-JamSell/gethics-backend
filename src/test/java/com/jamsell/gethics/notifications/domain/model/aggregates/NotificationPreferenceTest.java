package com.jamsell.gethics.notifications.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationPreferenceTest {

    @Test
    void newPreferenceDefaultsToPushEnabled() {
        var preference = new NotificationPreference(UUID.randomUUID());

        assertTrue(preference.isPushEnabled());
    }

    @Test
    void setPushEnabledTogglesTheFlag() {
        var preference = new NotificationPreference(UUID.randomUUID());

        preference.setPushEnabled(false);

        assertFalse(preference.isPushEnabled());
    }

    @Test
    void nullUserIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new NotificationPreference(null));
    }

    @Test
    void userIdIsKept() {
        var userId = UUID.randomUUID();

        var preference = new NotificationPreference(userId);

        assertEquals(userId, preference.getUserId());
    }
}
