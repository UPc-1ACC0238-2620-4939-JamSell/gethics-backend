package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;

/**
 * Domain Service. Trabaja unicamente con informacion YA CLASIFICADA: NO detecta tendencias, no cuenta eventos, no calcula
 * porcentajes ni ventanas temporales y no determina el nivel de riesgo a partir de datos sanitarios o financieros. Ese
 * algoritmo (y su umbral) sigue sin estar definido; cuando exista, sera quien produzca los niveles que aqui se evaluan.
 */
public class TrendAnalysisService {

    public boolean shouldGenerateAlert(RiskLevel riskLevel, AlertRiskPolicy policy) {
        return policy.generatesAlert(riskLevel);
    }
}
