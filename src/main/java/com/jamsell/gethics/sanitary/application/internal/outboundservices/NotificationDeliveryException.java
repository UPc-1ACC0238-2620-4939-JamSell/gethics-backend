package com.jamsell.gethics.sanitary.application.internal.outboundservices;

/** Los adapters de {@link NotificationService} deben envolver sus fallos de entrega en esta excepcion. */
public class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
