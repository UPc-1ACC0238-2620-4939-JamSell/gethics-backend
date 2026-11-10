package com.jamsell.gethics.livestock.domain.exceptions;

/** US-30: datos de venta invalidos (precio, fecha o dueno que no se puede determinar). */
public class InvalidAnimalSaleException extends RuntimeException {
    public InvalidAnimalSaleException(String message) {
        super(message);
    }
}
