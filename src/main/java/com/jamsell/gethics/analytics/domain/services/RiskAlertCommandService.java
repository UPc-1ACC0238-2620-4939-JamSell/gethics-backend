package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.commands.GenerateRiskAlertCommand;

import java.util.Optional;

public interface RiskAlertCommandService {
    /** Alerta creada, o vacio si la politica no la requiere o la tendencia ya tiene su alerta inicial. */
    Optional<Alert> handle(GenerateRiskAlertCommand command);
}
