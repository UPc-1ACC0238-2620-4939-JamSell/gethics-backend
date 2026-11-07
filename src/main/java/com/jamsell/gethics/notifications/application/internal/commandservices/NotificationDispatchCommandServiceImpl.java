package com.jamsell.gethics.notifications.application.internal.commandservices;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.domain.model.commands.SendPushNotificationCommand;
import com.jamsell.gethics.notifications.domain.model.valueobjects.PushNotificationDispatchOutcome;
import com.jamsell.gethics.notifications.domain.model.valueobjects.PushNotificationDispatchResult;
import com.jamsell.gethics.notifications.domain.services.NotificationDispatchCommandService;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationDeliveryException;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationGateway;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-22: envia una notificacion push respetando la preferencia del usuario (Escenario 2) antes de llamar al
 * {@link PushNotificationGateway} (Escenario 1). Un usuario sin preferencia guardada se trata como activo.
 */
@Slf4j
@Service
public class NotificationDispatchCommandServiceImpl implements NotificationDispatchCommandService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final PushNotificationGateway pushNotificationGateway;

    public NotificationDispatchCommandServiceImpl(NotificationPreferenceRepository notificationPreferenceRepository,
                                                   PushNotificationGateway pushNotificationGateway) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.pushNotificationGateway = pushNotificationGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public PushNotificationDispatchResult handle(SendPushNotificationCommand command) {
        var pushEnabled = notificationPreferenceRepository.findByUserId(command.userId())
                .map(NotificationPreference::isPushEnabled)
                .orElse(true);

        if (!pushEnabled) {
            log.info("Notificacion omitida para usuario={}: push desactivado", command.userId());
            return new PushNotificationDispatchResult(PushNotificationDispatchOutcome.SKIPPED_DISABLED);
        }

        try {
            pushNotificationGateway.send(new PushNotificationMessage(
                    command.userId(), command.category().name(), command.title(), command.body()));
        } catch (PushNotificationDeliveryException exception) {
            log.warn("Fallo el envio de la notificacion a usuario={}: {}", command.userId(), exception.getMessage());
            return new PushNotificationDispatchResult(PushNotificationDispatchOutcome.DELIVERY_FAILED);
        }

        return new PushNotificationDispatchResult(PushNotificationDispatchOutcome.SENT);
    }
}
