package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.commands.DispatchPendingAlertsCommand;

public interface AlertDispatchCommandService {
    void handle(DispatchPendingAlertsCommand command);
}
