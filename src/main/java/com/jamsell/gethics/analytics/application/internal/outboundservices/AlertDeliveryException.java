package com.jamsell.gethics.analytics.application.internal.outboundservices;

/** Los adapters de {@link PushNotificationService} deben envolver sus fallos de entrega en esta excepcion. */
public class AlertDeliveryException extends RuntimeException {
    public AlertDeliveryException(String message) {
        super(message);
    }

    public AlertDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
