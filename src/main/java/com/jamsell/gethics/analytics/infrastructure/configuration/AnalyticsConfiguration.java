package com.jamsell.gethics.analytics.infrastructure.configuration;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.services.AlertRiskPolicy;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

// Scheduling propio: el TrendAnalysisJob no depende de que otro bounded context lo habilite.
@Configuration
@EnableScheduling
public class AnalyticsConfiguration {

    @Bean
    public TrendAnalysisService trendAnalysisService() {
        return new TrendAnalysisService();
    }

    /**
     * Niveles de riesgo que generan alerta, desde {@code gethics.analytics.alert-risk-levels} (lista de RiskLevel en
     * mayusculas). DECISION PENDIENTE DEL EQUIPO: no hay valor por defecto. Ausente o vacio = ningun nivel genera alerta
     * (generacion automatica inactiva); la aplicacion arranca igualmente. Un valor que no sea un RiskLevel hace fallar el
     * arranque en vez de ignorarse.
     */
    @Bean
    public AlertRiskPolicy alertRiskPolicy(@Value("${gethics.analytics.alert-risk-levels:}") List<RiskLevel> alertRiskLevels) {
        return new ConfiguredAlertRiskPolicy(alertRiskLevels);
    }
}
