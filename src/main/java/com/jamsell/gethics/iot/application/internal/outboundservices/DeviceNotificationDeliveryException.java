package com.jamsell.gethics.iot.application.internal.outboundservices;

/** Los adapters de {@link DeviceNotificationService} deben envolver sus fallos de entrega en esta excepcion. */
public class DeviceNotificationDeliveryException extends RuntimeException {

    public DeviceNotificationDeliveryException(String message) {
        super(message);
    }

    public DeviceNotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
