package com.jamsell.gethics.subscription.domain.model.valueobjects;

import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import com.jamsell.gethics.subscription.domain.model.aggregates.Subscription;

public record CurrentSubscription(Plan plan, Subscription subscription) {

    public boolean isFreePlan() {
        return subscription == null;
    }
}
