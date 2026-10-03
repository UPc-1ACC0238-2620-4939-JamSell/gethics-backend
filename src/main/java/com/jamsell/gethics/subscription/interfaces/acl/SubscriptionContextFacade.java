package com.jamsell.gethics.subscription.interfaces.acl;

import com.jamsell.gethics.subscription.domain.model.queries.GetCurrentSubscriptionByUserIdQuery;
import com.jamsell.gethics.subscription.domain.services.SubscriptionQueryService;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionContextFacade {

    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionContextFacade(SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionQueryService = subscriptionQueryService;
    }

    public int fetchMaxAnimalsByUserId(Long userId) {
        return subscriptionQueryService.handle(new GetCurrentSubscriptionByUserIdQuery(userId))
                .map(current -> current.plan().getMaxAnimals())
                .orElse(0);
    }

    public boolean hasFeature(Long userId, String feature) {
        return subscriptionQueryService.handle(new GetCurrentSubscriptionByUserIdQuery(userId))
                .map(current -> current.plan().getFeatures().contains(feature))
                .orElse(false);
    }
}
