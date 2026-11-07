package com.jamsell.gethics.livestock.domain.exceptions;

public class DuplicateFarmNameException extends RuntimeException {
    public DuplicateFarmNameException(String name) {
        super("Ya tienes una granja llamada " + name + ".");
    }
}
