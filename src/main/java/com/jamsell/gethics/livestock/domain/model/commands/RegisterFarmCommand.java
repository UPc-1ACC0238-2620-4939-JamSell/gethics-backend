package com.jamsell.gethics.livestock.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterFarmCommand(
        UUID ownerId,
        String name,
        String location,
        BigDecimal sizeHectares) {
}
