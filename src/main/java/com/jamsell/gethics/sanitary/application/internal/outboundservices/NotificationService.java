package com.jamsell.gethics.sanitary.application.internal.outboundservices;

/**
 * Puerto de salida hacia el proveedor de notificaciones. Recibe la informacion sanitaria y {@code animalId} como
 * referencia; resolver animal -> propietario -> device token es responsabilidad del adapter real (requiere IAM).
 */
public interface NotificationService {

    /** @throws NotificationDeliveryException si la notificacion no pudo entregarse al proveedor */
    void sendVaccinationReminder(VaccinationReminderNotification notification);
}
