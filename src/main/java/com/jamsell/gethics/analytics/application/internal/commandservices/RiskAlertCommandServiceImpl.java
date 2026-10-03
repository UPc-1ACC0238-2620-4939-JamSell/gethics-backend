package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.domain.exceptions.LivestockTrendNotFoundException;
import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.commands.GenerateRiskAlertCommand;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import com.jamsell.gethics.analytics.domain.repositories.LivestockTrendRepository;
import com.jamsell.gethics.analytics.domain.services.AlertRiskPolicy;
import com.jamsell.gethics.analytics.domain.services.RiskAlertCommandService;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Genera la alerta inicial de una tendencia si la politica configurada lo requiere para el nivel de riesgo de la
 * TENDENCIA ({@code LivestockTrend.riskLevel}). El riesgo general ({@code Analytics.riskLevel}) no interviene: el informe
 * no define como se calcula, y US-21 evalua la tendencia concreta para decidir si genera una Alert. Sin politica
 * configurada ningun nivel genera alerta.
 * <p>
 * Idempotencia: si la tendencia ya tiene una alerta no se crea otra. Es una comprobacion a nivel de Application, sin
 * restriccion en la base de datos (el informe permite 0..* alertas por tendencia) y NO cubre carreras entre varias
 * instancias.
 */
@Service
public class RiskAlertCommandServiceImpl implements RiskAlertCommandService {

    private final LivestockTrendRepository trendRepository;
    private final AlertRepository alertRepository;
    private final TrendAnalysisService trendAnalysisService;
    private final AlertRiskPolicy policy;

    public RiskAlertCommandServiceImpl(LivestockTrendRepository trendRepository, AlertRepository alertRepository,
                                       TrendAnalysisService trendAnalysisService, AlertRiskPolicy policy) {
        this.trendRepository = trendRepository;
        this.alertRepository = alertRepository;
        this.trendAnalysisService = trendAnalysisService;
        this.policy = policy;
    }

    @Override
    @Transactional
    public Optional<Alert> handle(GenerateRiskAlertCommand command) {
        var trend = trendRepository.findById(command.trendId())
                .orElseThrow(() -> new LivestockTrendNotFoundException(command.trendId()));
        if (!trendAnalysisService.shouldGenerateAlert(trend.getRiskLevel(), policy)) {
            return Optional.empty();
        }
        if (alertRepository.existsByTrendId(trend.getId())) {
            return Optional.empty();
        }
        var ownerId = trend.getAnalytics().getOwnerId();
        return Optional.of(alertRepository.save(Alert.create(ownerId, trend, command.message())));
    }
}
