package com.jamsell.gethics.subscription.domain.services;

import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import com.jamsell.gethics.subscription.domain.model.queries.GetAllPlansQuery;
import com.jamsell.gethics.subscription.domain.model.queries.GetCurrentSubscriptionByUserIdQuery;
import com.jamsell.gethics.subscription.domain.model.valueobjects.CurrentSubscription;

import java.util.List;
import java.util.Optional;

public interface SubscriptionQueryService {

    List<Plan> handle(GetAllPlansQuery query);

    Optional<CurrentSubscription> handle(GetCurrentSubscriptionByUserIdQuery query);
}
