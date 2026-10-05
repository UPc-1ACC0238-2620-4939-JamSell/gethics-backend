package com.jamsell.gethics.iot.infrastructure.notifications;

import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceDisconnectionNotification;
import com.jamsell.gethics.iot.application.internal.outboundservices.DeviceNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter TEMPORAL de {@link DeviceNotificationService}: solo escribe en el log. <b>NO es un push real</b>: no hay
 * proveedor (FCM u otro), ni credenciales, ni device token, ni forma de resolver el propietario (IAM pendiente). Un
 * adapter real debera reemplazarlo, resolviendo ownerId -&gt; device token.
 */
@Slf4j
@Component
public class LoggingDeviceNotificationAdapter implements DeviceNotificationService {

    @Override
    public void send(DeviceDisconnectionNotification notification) {
        log.info("[SIMULADO - no es un push real] Dispositivo desconectado: owner={}, dispositivo={}, codigo={}, "
                        + "mensaje={}",
                notification.ownerId(), notification.deviceId(), notification.code(), notification.message());
    }
}
