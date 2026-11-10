package com.jamsell.gethics.veterinary.domain.model.valueobjects;

public enum CareSyncStatus {
    CREATED,
    DUPLICATE,
    CONFLICT,
    REJECTED,
    /** La atencion quedo guardada pero su historial clinico no se pudo actualizar: reintentar con el mismo id. */
    FAILED
}
