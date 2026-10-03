package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueban solo el mecanismo (la politica decide); los conjuntos de niveles usados aqui son valores de prueba, no una
 * propuesta de negocio. No hay logica de deteccion que probar: no existe.
 */
class TrendAnalysisServiceTest {

    private final TrendAnalysisService service = new TrendAnalysisService();

    private static AlertRiskPolicy policyOf(Set<RiskLevel> levels) {
        return levels::contains;
    }

    @Test
    void levelIncludedInThePolicyGeneratesAlert() {
        assertTrue(service.shouldGenerateAlert(RiskLevel.MEDIUM, policyOf(Set.of(RiskLevel.MEDIUM))));
    }

    @Test
    void levelNotIncludedDoesNotGenerateAlert() {
        assertFalse(service.shouldGenerateAlert(RiskLevel.LOW, policyOf(Set.of(RiskLevel.MEDIUM))));
    }

    @Test
    void emptyPolicyNeverGeneratesAlert() {
        for (var level : RiskLevel.values()) {
            assertFalse(service.shouldGenerateAlert(level, policyOf(Set.of())));
        }
    }

    @Test
    void changingThePolicyChangesTheOutcomeWithoutTouchingTheDomain() {
        assertFalse(service.shouldGenerateAlert(RiskLevel.HIGH, policyOf(Set.of(RiskLevel.CRITICAL))));
        assertTrue(service.shouldGenerateAlert(RiskLevel.HIGH, policyOf(Set.of(RiskLevel.HIGH, RiskLevel.CRITICAL))));
        assertTrue(service.shouldGenerateAlert(RiskLevel.HIGH, policyOf(Set.of(RiskLevel.HIGH))));
    }
}
