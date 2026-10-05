package com.jamsell.gethics.livestock.domain.model.queries;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

import java.util.UUID;

/** Animales de una granja. Igual que el inventario (US-06), sin {@code status} solo se listan los ACTIVE. */
public record GetAnimalsByFarmQuery(UUID farmId, AnimalStatus status) {

    public static GetAnimalsByFarmQuery of(UUID farmId, AnimalStatus status) {
        return new GetAnimalsByFarmQuery(farmId, status == null ? AnimalStatus.ACTIVE : status);
    }
}
