package com.jamsell.gethics.livestock.domain.exceptions;

import java.util.UUID;

public class AnimalNotFoundException extends RuntimeException {
    public AnimalNotFoundException() {
        super("El animal no existe.");
    }

    public AnimalNotFoundException(UUID animalId) {
        super("No se encontro el animal " + animalId + ".");
    }
}
