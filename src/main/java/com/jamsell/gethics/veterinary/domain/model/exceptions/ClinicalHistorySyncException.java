package com.jamsell.gethics.veterinary.domain.model.exceptions;

/**
 * La atencion quedo guardada pero no se pudo agregar al historial clinico. Es reintentable: al repetir la misma
 * solicitud (mismo clientRequestId) se vuelve a intentar sin duplicar nada.
 */
public class ClinicalHistorySyncException extends RuntimeException {
    public ClinicalHistorySyncException(Throwable cause) {
        super("The care was saved but could not be added to the clinical history yet; retry the same request", cause);
    }
}
