package com.jamsell.gethics.analytics.infrastructure.configuration;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.services.AlertRiskPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Los niveles usados en las propiedades son valores de prueba: el negocio aun no definio cuales generan alerta. */
class AnalyticsConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AnalyticsConfiguration.class);

    @Test
    void withoutConfigurationNoLevelGeneratesAlertAndTheContextStarts() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            var policy = context.getBean(AlertRiskPolicy.class);
            for (var level : RiskLevel.values()) {
                assertFalse(policy.generatesAlert(level), level + " no debe generar alerta sin configuracion");
            }
        });
    }

    @Test
    void emptyPropertyBehavesAsNotConfigured() {
        runner.withPropertyValues("gethics.analytics.alert-risk-levels=").run(context -> {
            var policy = context.getBean(AlertRiskPolicy.class);
            for (var level : RiskLevel.values()) {
                assertFalse(policy.generatesAlert(level));
            }
        });
    }

    @Test
    void configuredLevelsAreTheOnlyOnesThatGenerateAlert() {
        runner.withPropertyValues("gethics.analytics.alert-risk-levels=MEDIUM,CRITICAL").run(context -> {
            var policy = context.getBean(AlertRiskPolicy.class);
            assertFalse(policy.generatesAlert(RiskLevel.LOW));
            assertTrue(policy.generatesAlert(RiskLevel.MEDIUM));
            assertFalse(policy.generatesAlert(RiskLevel.HIGH));
            assertTrue(policy.generatesAlert(RiskLevel.CRITICAL));
        });
    }

    @Test
    void anotherConfigurationChangesTheOutcome() {
        runner.withPropertyValues("gethics.analytics.alert-risk-levels=LOW").run(context ->
                assertTrue(context.getBean(AlertRiskPolicy.class).generatesAlert(RiskLevel.LOW)));
        runner.withPropertyValues("gethics.analytics.alert-risk-levels=HIGH").run(context ->
                assertFalse(context.getBean(AlertRiskPolicy.class).generatesAlert(RiskLevel.LOW)));
    }

    @Test
    void unknownLevelFailsTheStartupInsteadOfBeingIgnored() {
        runner.withPropertyValues("gethics.analytics.alert-risk-levels=EXTREME")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void emptyCollectionNeverAlertsAndASingleLevelOnlyAlertsForItself() {
        var empty = new ConfiguredAlertRiskPolicy(List.of());
        var one = new ConfiguredAlertRiskPolicy(List.of(RiskLevel.HIGH));

        assertFalse(empty.generatesAlert(RiskLevel.HIGH));
        assertTrue(one.generatesAlert(RiskLevel.HIGH));
        assertFalse(one.generatesAlert(RiskLevel.CRITICAL));
    }
}
