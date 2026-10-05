package com.jamsell.gethics.notifications.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.commands.SendPushNotificationCommand;
import com.jamsell.gethics.notifications.domain.model.valueobjects.NotificationCategory;
import com.jamsell.gethics.notifications.domain.model.valueobjects.PushNotificationDispatchOutcome;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationDeliveryException;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationGateway;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationMessage;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDispatchCommandServiceImplTest {

    private final NotificationPreferenceRepository preferenceRepository = mock(NotificationPreferenceRepository.class);
    private final PushNotificationGateway gateway = mock(PushNotificationGateway.class);
    private final NotificationDispatchCommandServiceImpl service =
            new NotificationDispatchCommandServiceImpl(preferenceRepository, gateway);

    private final UUID userId = UUID.randomUUID();
    private final SendPushNotificationCommand command =
            new SendPushNotificationCommand(userId, NotificationCategory.SANITARY, "Vacuna pendiente", "Aftosa vence manana");

    @Test
    void sendsWhenUserHasNoStoredPreference() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var result = service.handle(command);

        assertEquals(PushNotificationDispatchOutcome.SENT, result.outcome());
        assertTrue(result.wasSent());
        verify(gateway).send(new PushNotificationMessage(userId, "SANITARY", "Vacuna pendiente", "Aftosa vence manana"));
    }

    @Test
    void skipsWhenUserDisabledPushNotifications() {
        var preference = new NotificationPreference(userId);
        preference.setPushEnabled(false);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        var result = service.handle(command);

        assertEquals(PushNotificationDispatchOutcome.SKIPPED_DISABLED, result.outcome());
        assertFalse(result.wasSent());
        verify(gateway, never()).send(any());
    }

    @Test
    void returnsDeliveryFailedWhenGatewayThrows() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        doThrow(new PushNotificationDeliveryException("provider down")).when(gateway).send(any());

        var result = service.handle(command);

        assertEquals(PushNotificationDispatchOutcome.DELIVERY_FAILED, result.outcome());
        assertFalse(result.wasSent());
    }
}
