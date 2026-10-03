package com.jamsell.gethics.analytics.infrastructure.notifications;

import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertNotification;
import com.jamsell.gethics.analytics.application.internal.outboundservices.PushNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter TEMPORAL de {@link PushNotificationService}: solo escribe en el log. <b>NO es un push real</b>: no hay proveedor
 * (FCM u otro), ni credenciales, ni device token, ni forma de resolver el propietario (IAM pendiente). Un adapter real
 * debera reemplazarlo, resolviendo ownerId -> device token.
 */
@Slf4j
@Component
public class LoggingPushNotificationAdapter implements PushNotificationService {

    @Override
    public void send(AlertNotification n) {
        log.info("[SIMULADO - no es un push real] Alerta de ganado: owner={}, alerta={}, mensaje={}",
                n.ownerId(), n.alertId(), n.message());
    }
}
