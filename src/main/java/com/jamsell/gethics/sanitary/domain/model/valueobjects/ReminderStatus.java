package com.jamsell.gethics.sanitary.domain.model.valueobjects;

/**
 * Ciclo de entrega de un recordatorio. El informe define la columna {@code status} pero no enumera sus valores:
 * estos tres son una decision de implementacion.
 */
public enum ReminderStatus {
    /** Reclamado (persistido) pero aun sin confirmacion de envio. Un PENDING huerfano no se reintenta. */
    PENDING,
    SENT,
    /** El envio fallo con {@code NotificationDeliveryException}; es reintentable mientras el evento siga vigente. */
    FAILED
}
