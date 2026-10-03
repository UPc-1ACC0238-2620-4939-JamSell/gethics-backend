package com.jamsell.gethics.subscription.interfaces.rest.transform;

import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import com.jamsell.gethics.subscription.interfaces.rest.resources.PlanResource;

import java.util.List;

public class PlanResourceFromEntityAssembler {

    public static PlanResource toResourceFromEntity(Plan plan) {
        return new PlanResource(
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getMaxAnimals(),
                List.copyOf(plan.getFeatures())
        );
    }
}
