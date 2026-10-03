package com.jamsell.gethics.analytics.domain.model.valueobjects;

/**
 * Estados definidos en el informe. PENDING = aun no enviada (tambien lo queda una alerta cuyo envio fallo: no existe un
 * estado FAILED). READ aun no tiene comportamiento: marcar una alerta como leida no forma parte de esta US.
 */
public enum AlertStatus {
    PENDING,
    SENT,
    READ
}
