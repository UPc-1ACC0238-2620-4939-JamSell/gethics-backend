package com.jamsell.gethics.subscription.domain.services;

import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.application.result.Result;
import com.jamsell.gethics.subscription.domain.model.commands.SeedDefaultPlansCommand;
import com.jamsell.gethics.subscription.domain.model.commands.SubscribeToPlanCommand;
import com.jamsell.gethics.subscription.domain.model.valueobjects.CurrentSubscription;

public interface SubscriptionCommandService {

    Result<CurrentSubscription, ApplicationError> handle(SubscribeToPlanCommand command);

    void handle(SeedDefaultPlansCommand command);
}
