package com.jamsell.gethics.shared.application.internal.outboundservices;

/** Los adapters de {@link PushNotificationGateway} deben envolver sus fallos de entrega en esta excepcion. */
public class PushNotificationDeliveryException extends RuntimeException {
    public PushNotificationDeliveryException(String message) {
        super(message);
    }

    public PushNotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
