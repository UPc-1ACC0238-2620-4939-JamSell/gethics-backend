package com.jamsell.gethics.livestock.domain.model.queries;

import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalDataException;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

/**
 * Inventario (US-06). {@code search} es opcional (null = sin busqueda) y compara por contenido, sin distinguir
 * mayusculas, contra arete, nombre y raza. Sin {@code status} solo se listan los animales ACTIVE: los vendidos o dados
 * de baja salen de la lista activa (US-08).
 */
public record GetAnimalsQuery(String search, AnimalStatus status) {

    static final int SEARCH_MAX_LENGTH = 100;

    public static GetAnimalsQuery of(String search, AnimalStatus status) {
        var criterion = search == null || search.isBlank() ? null : search.trim();
        if (criterion != null && criterion.length() > SEARCH_MAX_LENGTH) {
            throw new InvalidAnimalDataException("El criterio de busqueda no puede superar 100 caracteres.");
        }
        return new GetAnimalsQuery(criterion, status == null ? AnimalStatus.ACTIVE : status);
    }
}
