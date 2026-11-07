package com.jamsell.gethics.livestock.interfaces.rest.resources;

import com.jamsell.gethics.livestock.domain.model.valueobjects.FarmStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record FarmResource(
        UUID id,
        UUID ownerId,
        String name,
        String location,
        BigDecimal sizeHectares,
        FarmStatus status) {
}
