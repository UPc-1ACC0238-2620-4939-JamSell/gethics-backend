package com.jamsell.gethics.shared.infrastructure.notifications;

import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationGateway;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter TEMPORAL de {@link PushNotificationGateway}: solo escribe en el log. <b>NO es un push real</b>: no hay
 * proveedor (FCM u otro), ni credenciales, ni device token. Un adapter real debera reemplazarlo, resolviendo
 * userId -> device token una vez que IAM exista.
 */
@Slf4j
@Component
public class LoggingPushNotificationGateway implements PushNotificationGateway {

    @Override
    public void send(PushNotificationMessage message) {
        log.info("[SIMULADO - no es un push real] Notificacion {}: usuario={}, titulo={}, cuerpo={}",
                message.category(), message.userId(), message.title(), message.body());
    }
}
