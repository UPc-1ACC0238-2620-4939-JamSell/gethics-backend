package com.jamsell.gethics.notifications.domain.model.valueobjects;

/** Resultado de intentar despachar una notificacion push general (US-22). */
public enum PushNotificationDispatchOutcome {
    /** Se envio al gateway de push. */
    SENT,
    /** No se envio porque el usuario desactivo las notificaciones push (Escenario 2). */
    SKIPPED_DISABLED,
    /** El usuario tiene las notificaciones activas pero el gateway de push fallo al entregarla. */
    DELIVERY_FAILED
}
