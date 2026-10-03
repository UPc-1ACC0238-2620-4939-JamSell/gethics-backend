package com.jamsell.gethics.subscription.application.internal.eventhandlers;

import com.jamsell.gethics.subscription.domain.model.commands.SeedDefaultPlansCommand;
import com.jamsell.gethics.subscription.domain.services.SubscriptionCommandService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class PlanSeedEventHandler {

    private final SubscriptionCommandService subscriptionCommandService;

    public PlanSeedEventHandler(SubscriptionCommandService subscriptionCommandService) {
        this.subscriptionCommandService = subscriptionCommandService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void on(ApplicationReadyEvent event) {
        subscriptionCommandService.handle(new SeedDefaultPlansCommand());
    }
}
