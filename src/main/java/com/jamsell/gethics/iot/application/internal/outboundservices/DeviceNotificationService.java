package com.jamsell.gethics.iot.application.internal.outboundservices;

/**
 * Puerto de salida propio de IoT hacia el proveedor de push (no reutiliza nada de otros bounded contexts, igual que
 * {@code analytics.PushNotificationService}). Resolver ownerId -&gt; device token es responsabilidad del adapter
 * real y requiere IAM, que aun no existe.
 */
public interface DeviceNotificationService {

    /** @throws DeviceNotificationDeliveryException si la notificacion no pudo entregarse al proveedor */
    void send(DeviceDisconnectionNotification notification);
}
