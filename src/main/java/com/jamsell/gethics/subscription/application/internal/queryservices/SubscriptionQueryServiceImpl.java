package com.jamsell.gethics.subscription.application.internal.queryservices;

import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import com.jamsell.gethics.subscription.domain.model.aggregates.Subscription;
import com.jamsell.gethics.subscription.domain.model.queries.GetAllPlansQuery;
import com.jamsell.gethics.subscription.domain.model.queries.GetCurrentSubscriptionByUserIdQuery;
import com.jamsell.gethics.subscription.domain.model.valueobjects.CurrentSubscription;
import com.jamsell.gethics.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.jamsell.gethics.subscription.domain.services.SubscriptionQueryService;
import com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionQueryServiceImpl(PlanRepository planRepository, SubscriptionRepository subscriptionRepository) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<Plan> handle(GetAllPlansQuery query) {
        return planRepository.findAllByOrderByPriceAsc();
    }

    @Override
    public Optional<CurrentSubscription> handle(GetCurrentSubscriptionByUserIdQuery query) {
        var now = Instant.now();
        var current = subscriptionRepository.findByUserIdAndStatus(query.userId(), SubscriptionStatus.ACTIVE).stream()
                .filter(subscription -> subscription.isActiveAt(now))
                .max(Comparator.comparing(Subscription::getStartedAt));
        if (current.isPresent()) {
            return planRepository.findById(current.get().getPlanId())
                    .map(plan -> new CurrentSubscription(plan, current.get()));
        }
        return planRepository.findByCode(Plan.FREE_CODE).map(plan -> new CurrentSubscription(plan, null));
    }
}
