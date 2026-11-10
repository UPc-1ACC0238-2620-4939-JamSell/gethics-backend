package com.jamsell.gethics.livestock.domain.model.queries;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalQrNotFoundException;

import java.util.Locale;

/** US-29: busca un animal por el codigo de su QR (sin espacios sobrantes y sin distinguir mayusculas). */
public record GetAnimalByQrCodeQuery(String code) {

    public GetAnimalByQrCodeQuery {
        if (code == null || code.isBlank()) {
            throw new AnimalQrNotFoundException();
        }
        code = code.trim().toUpperCase(Locale.ROOT);
    }
}
