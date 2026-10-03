package com.jamsell.gethics.subscription.interfaces.rest;

import com.jamsell.gethics.subscription.domain.model.queries.GetAllPlansQuery;
import com.jamsell.gethics.subscription.domain.services.SubscriptionQueryService;
import com.jamsell.gethics.subscription.interfaces.rest.resources.PlanResource;
import com.jamsell.gethics.subscription.interfaces.rest.transform.PlanResourceFromEntityAssembler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/plans", produces = MediaType.APPLICATION_JSON_VALUE)
public class PlanController {

    private final SubscriptionQueryService subscriptionQueryService;

    public PlanController(SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @GetMapping
    public List<PlanResource> getPlans() {
        return subscriptionQueryService.handle(new GetAllPlansQuery()).stream()
                .map(PlanResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
    }
}
