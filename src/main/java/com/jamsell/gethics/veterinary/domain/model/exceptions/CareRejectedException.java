package com.jamsell.gethics.veterinary.domain.model.exceptions;

/** La atencion no se puede registrar por una regla de negocio (paciente invalido, sin permiso...). No es reintentable. */
public abstract class CareRejectedException extends RuntimeException {
    protected CareRejectedException(String message) {
        super(message);
    }
}
