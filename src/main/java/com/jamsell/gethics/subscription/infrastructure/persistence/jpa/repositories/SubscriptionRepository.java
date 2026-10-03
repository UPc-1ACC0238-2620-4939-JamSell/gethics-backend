package com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.subscription.domain.model.aggregates.Subscription;
import com.jamsell.gethics.subscription.domain.model.valueobjects.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
}
