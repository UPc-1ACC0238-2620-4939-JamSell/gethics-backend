package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;

/**
 * Politica que responde si un nivel de riesgo genera alerta. DECISION PENDIENTE DEL EQUIPO: el informe no define que
 * niveles disparan alertas, asi que el dominio no asume ninguno; la implementacion se configura desde fuera.
 */
public interface AlertRiskPolicy {
    boolean generatesAlert(RiskLevel riskLevel);
}
