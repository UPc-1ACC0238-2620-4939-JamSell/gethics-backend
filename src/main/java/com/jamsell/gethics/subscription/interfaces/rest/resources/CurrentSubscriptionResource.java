package com.jamsell.gethics.subscription.interfaces.rest.resources;

import java.time.Instant;

public record CurrentSubscriptionResource(
        PlanResource plan,
        String status,
        Instant startedAt,
        Instant endsAt,
        String paymentReference
) {
}
