package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.commands.AnalyzeLivestockTrendsCommand;

public interface TrendAnalysisCommandService {
    void handle(AnalyzeLivestockTrendsCommand command);
}
