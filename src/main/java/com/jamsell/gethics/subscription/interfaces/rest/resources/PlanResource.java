package com.jamsell.gethics.subscription.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

public record PlanResource(
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int maxAnimals,
        List<String> features
) {
}
