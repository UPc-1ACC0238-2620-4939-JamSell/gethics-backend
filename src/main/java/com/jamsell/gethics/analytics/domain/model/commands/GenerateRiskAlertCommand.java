package com.jamsell.gethics.analytics.domain.model.commands;

import java.util.UUID;

public record GenerateRiskAlertCommand(UUID trendId, String message) {
}
