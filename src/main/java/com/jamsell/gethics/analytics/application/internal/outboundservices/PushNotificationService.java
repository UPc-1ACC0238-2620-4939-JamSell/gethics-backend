package com.jamsell.gethics.analytics.application.internal.outboundservices;

/**
 * Puerto de salida propio de Analytics &amp; Alerts hacia el proveedor de push (no reutiliza nada de otros bounded
 * contexts). El contrato usa {@code ownerId} como referencia: resolver propietario -> device token es responsabilidad del
 * adapter real y requiere IAM, que aun no existe.
 */
public interface PushNotificationService {

    /** @throws AlertDeliveryException si la notificacion no pudo entregarse al proveedor */
    void send(AlertNotification notification);
}
