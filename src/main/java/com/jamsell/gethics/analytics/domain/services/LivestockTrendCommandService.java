package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.commands.RegisterClassifiedTrendCommand;
import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;

public interface LivestockTrendCommandService {
    LivestockTrend handle(RegisterClassifiedTrendCommand command);
}
