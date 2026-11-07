package com.jamsell.gethics.livestock.domain.exceptions;

public class DuplicateAnimalTagException extends RuntimeException {
    public DuplicateAnimalTagException(String tag) {
        super("Ya existe un animal con el arete " + tag + ".");
    }
}
