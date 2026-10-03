package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.commands.GenerateRiskAlertCommand;
import com.jamsell.gethics.analytics.domain.model.commands.RegisterClassifiedTrendCommand;
import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.repositories.AnalyticsRepository;
import com.jamsell.gethics.analytics.domain.services.LivestockTrendCommandService;
import com.jamsell.gethics.analytics.domain.services.RiskAlertCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un resultado de analisis ya CLASIFICADO (nada se calcula aqui) y delega la generacion de la alerta. Sustituye,
 * hasta que exista el clasificador real, la entrada del futuro AnalyzeLivestockTrendCommandHandler.
 * <p>
 * Limitaciones conocidas: (1) registrar dos veces el mismo resultado crea dos tendencias: no hay una clave natural que las
 * identifique como "el mismo patron" (definicion pendiente); (2) la busqueda-o-creacion del Analytics del propietario no
 * esta protegida frente a dos ejecuciones simultaneas: la restriccion UNIQUE(owner_id) haria fallar a una de ellas.
 */
@Service
public class LivestockTrendCommandServiceImpl implements LivestockTrendCommandService {

    private final AnalyticsRepository analyticsRepository;
    private final RiskAlertCommandService riskAlertService;

    public LivestockTrendCommandServiceImpl(AnalyticsRepository analyticsRepository, RiskAlertCommandService riskAlertService) {
        this.analyticsRepository = analyticsRepository;
        this.riskAlertService = riskAlertService;
    }

    @Override
    @Transactional
    public LivestockTrend handle(RegisterClassifiedTrendCommand command) {
        var analytics = analyticsRepository.findByOwnerId(command.ownerId())
                .orElseGet(() -> new Analytics(command.ownerId(), command.overallRiskLevel()));
        analytics.registerTrend(command.trendType(), command.trendRiskLevel(), command.description(),
                command.detectedAt(), command.overallRiskLevel());
        var saved = analyticsRepository.save(analytics);
        // La tendencia recien registrada es la ultima; se toma de la instancia persistida (con id asignado).
        var trend = saved.getTrends().getLast();
        riskAlertService.handle(new GenerateRiskAlertCommand(trend.getId(), command.alertMessage()));
        return trend;
    }
}
