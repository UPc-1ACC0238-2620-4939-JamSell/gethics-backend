package com.jamsell.gethics.subscription.domain.model.aggregates;

import com.jamsell.gethics.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.jamsell.gethics.subscription.domain.model.valueobjects.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends AuditableAbstractAggregateRoot<Subscription> {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Column(length = 100)
    private String paymentReference;

    public Subscription(Long userId, Long planId, Instant startedAt, Instant endsAt, String paymentReference) {
        this.userId = userId;
        this.planId = planId;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
        this.paymentReference = paymentReference;
    }

    public boolean isActiveAt(Instant now) {
        return status == SubscriptionStatus.ACTIVE && now.isBefore(endsAt);
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }
}
