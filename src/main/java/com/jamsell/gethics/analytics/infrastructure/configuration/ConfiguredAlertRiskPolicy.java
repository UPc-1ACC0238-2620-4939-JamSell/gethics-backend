package com.jamsell.gethics.analytics.infrastructure.configuration;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.services.AlertRiskPolicy;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Politica configurable: un nivel genera alerta solo si esta explicitamente en el conjunto. Conjunto vacio = nunca. */
public final class ConfiguredAlertRiskPolicy implements AlertRiskPolicy {

    private final Set<RiskLevel> levels;

    public ConfiguredAlertRiskPolicy(Collection<RiskLevel> levels) {
        this.levels = levels.isEmpty() ? EnumSet.noneOf(RiskLevel.class) : EnumSet.copyOf(levels);
    }

    @Override
    public boolean generatesAlert(RiskLevel riskLevel) {
        return levels.contains(riskLevel);
    }
}
