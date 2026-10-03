package com.jamsell.gethics.subscription.interfaces.rest.transform;

import com.jamsell.gethics.subscription.domain.model.valueobjects.CurrentSubscription;
import com.jamsell.gethics.subscription.interfaces.rest.resources.CurrentSubscriptionResource;

public class CurrentSubscriptionResourceFromEntityAssembler {

    public static CurrentSubscriptionResource toResourceFromEntity(CurrentSubscription current) {
        var plan = PlanResourceFromEntityAssembler.toResourceFromEntity(current.plan());
        if (current.isFreePlan()) {
            return new CurrentSubscriptionResource(plan, "FREE", null, null, null);
        }
        var subscription = current.subscription();
        return new CurrentSubscriptionResource(
                plan,
                subscription.getStatus().name(),
                subscription.getStartedAt(),
                subscription.getEndsAt(),
                subscription.getPaymentReference()
        );
    }
}
