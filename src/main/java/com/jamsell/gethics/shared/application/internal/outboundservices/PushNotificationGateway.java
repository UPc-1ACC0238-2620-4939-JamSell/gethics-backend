package com.jamsell.gethics.shared.application.internal.outboundservices;

/**
 * Puerto de salida GENERAL (US-22) hacia el proveedor de notificaciones push, para cualquier bounded context que ya
 * resuelva el {@code userId} destinatario. Sanitary y Analytics mantienen sus propios puertos locales
 * ({@code NotificationService}, {@code PushNotificationService}) porque sus eventos solo conocen
 * {@code animalId}/{@code ownerId}, no un usuario IAM; cuando IAM modele usuarios y device tokens, ambos pueden
 * migrar a este puerto en vez de mantener adapters duplicados.
 */
public interface PushNotificationGateway {

    /** @throws PushNotificationDeliveryException si la notificacion no pudo entregarse al proveedor */
    void send(PushNotificationMessage message);
}
