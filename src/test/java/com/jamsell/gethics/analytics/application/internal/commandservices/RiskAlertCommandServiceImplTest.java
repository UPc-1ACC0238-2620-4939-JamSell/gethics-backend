package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.domain.exceptions.LivestockTrendNotFoundException;
import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.commands.GenerateRiskAlertCommand;
import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import com.jamsell.gethics.analytics.domain.repositories.LivestockTrendRepository;
import com.jamsell.gethics.analytics.domain.services.AlertRiskPolicy;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Los conjuntos de niveles son valores de prueba: el negocio aun no definio que niveles generan alerta. */
class RiskAlertCommandServiceImplTest {

    private final LivestockTrendRepository trendRepository = mock(LivestockTrendRepository.class);
    private final AlertRepository alertRepository = mock(AlertRepository.class);
    private final UUID ownerId = UUID.randomUUID();
    private final UUID trendId = UUID.randomUUID();

    RiskAlertCommandServiceImplTest() {
        when(alertRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private RiskAlertCommandServiceImpl serviceWith(Set<RiskLevel> alertingLevels) {
        AlertRiskPolicy policy = alertingLevels::contains;
        return new RiskAlertCommandServiceImpl(trendRepository, alertRepository, new TrendAnalysisService(), policy);
    }

    private LivestockTrend trend(RiskLevel overall, RiskLevel trendLevel) {
        var trend = new Analytics(ownerId, overall)
                .registerTrend(TrendType.SANITARY, trendLevel, "d", Instant.now(), overall);
        ReflectionTestUtils.setField(trend, "id", trendId);
        when(trendRepository.findById(trendId)).thenReturn(Optional.of(trend));
        return trend;
    }

    private GenerateRiskAlertCommand command() {
        return new GenerateRiskAlertCommand(trendId, "mensaje de prueba");
    }

    @Test
    void levelIncludedInThePolicyGeneratesAPendingAlert() {
        var trend = trend(RiskLevel.LOW, RiskLevel.HIGH);

        var alert = serviceWith(Set.of(RiskLevel.HIGH)).handle(command());

        assertTrue(alert.isPresent());
        var saved = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(saved.capture());
        assertEquals(AlertStatus.PENDING, saved.getValue().getStatus());
        assertEquals(ownerId, saved.getValue().getOwnerId());
        assertSame(trend, saved.getValue().getTrend());
        assertEquals("mensaje de prueba", saved.getValue().getMessage());
    }

    @Test
    void levelNotIncludedGeneratesNothing() {
        trend(RiskLevel.LOW, RiskLevel.MEDIUM);

        var alert = serviceWith(Set.of(RiskLevel.HIGH)).handle(command());

        assertTrue(alert.isEmpty());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void emptyPolicyGeneratesNothingForAnyLevel() {
        for (var level : RiskLevel.values()) {
            trend(RiskLevel.CRITICAL, level);

            assertTrue(serviceWith(Set.of()).handle(command()).isEmpty());
        }
        verify(alertRepository, never()).save(any());
    }

    @Test
    void changingThePolicyChangesTheOutcome() {
        trend(RiskLevel.LOW, RiskLevel.MEDIUM);

        assertTrue(serviceWith(Set.of(RiskLevel.HIGH)).handle(command()).isEmpty());
        assertTrue(serviceWith(Set.of(RiskLevel.MEDIUM)).handle(command()).isPresent());
    }

    @Test
    void theTrendLevelDecidesNotTheOverallLevel() {
        trend(RiskLevel.CRITICAL, RiskLevel.LOW);
        assertTrue(serviceWith(Set.of(RiskLevel.CRITICAL)).handle(command()).isEmpty());

        trend(RiskLevel.LOW, RiskLevel.CRITICAL);
        assertTrue(serviceWith(Set.of(RiskLevel.CRITICAL)).handle(command()).isPresent());
    }

    @Test
    void trendThatAlreadyHasAnAlertDoesNotGetAnotherInitialOne() {
        trend(RiskLevel.LOW, RiskLevel.HIGH);
        when(alertRepository.existsByTrendId(trendId)).thenReturn(true);

        var alert = serviceWith(Set.of(RiskLevel.HIGH)).handle(command());

        assertTrue(alert.isEmpty());
        verify(alertRepository, never()).save(any());
    }

    @Test
    void unknownTrendIsReported() {
        when(trendRepository.findById(trendId)).thenReturn(Optional.empty());

        assertThrows(LivestockTrendNotFoundException.class, () -> serviceWith(Set.of(RiskLevel.HIGH)).handle(command()));
        verifyNoInteractions(alertRepository);
    }
}
