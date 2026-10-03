package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.commands.GenerateRiskAlertCommand;
import com.jamsell.gethics.analytics.domain.model.commands.RegisterClassifiedTrendCommand;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.repositories.AnalyticsRepository;
import com.jamsell.gethics.analytics.domain.services.RiskAlertCommandService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LivestockTrendCommandServiceImplTest {

    private final AnalyticsRepository analyticsRepository = mock(AnalyticsRepository.class);
    private final RiskAlertCommandService riskAlertService = mock(RiskAlertCommandService.class);
    private final LivestockTrendCommandServiceImpl service =
            new LivestockTrendCommandServiceImpl(analyticsRepository, riskAlertService);
    private final UUID ownerId = UUID.randomUUID();
    private final Instant detectedAt = Instant.parse("2026-10-01T10:00:00Z");

    LivestockTrendCommandServiceImplTest() {
        when(analyticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private RegisterClassifiedTrendCommand command(RiskLevel overall, RiskLevel trend) {
        return new RegisterClassifiedTrendCommand(ownerId, overall, TrendType.SANITARY, trend, "descripcion", detectedAt, "mensaje de prueba");
    }

    @Test
    void createsTheOwnersAnalyticsWithTheReceivedOverallRiskAndRegistersTheTrend() {
        when(analyticsRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        var trend = service.handle(command(RiskLevel.MEDIUM, RiskLevel.HIGH));

        var saved = ArgumentCaptor.forClass(Analytics.class);
        verify(analyticsRepository).save(saved.capture());
        assertEquals(ownerId, saved.getValue().getOwnerId());
        assertEquals(RiskLevel.MEDIUM, saved.getValue().getRiskLevel());
        assertEquals(1, saved.getValue().getTrends().size());
        assertSame(trend, saved.getValue().getTrends().getFirst());
        assertEquals(RiskLevel.HIGH, trend.getRiskLevel());
        assertEquals(TrendType.SANITARY, trend.getType());
        assertEquals(detectedAt, trend.getDetectedAt());
    }

    @Test
    void reusesTheExistingAnalyticsOfTheOwner() {
        var existing = new Analytics(ownerId, RiskLevel.LOW);
        when(analyticsRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(existing));

        service.handle(command(RiskLevel.HIGH, RiskLevel.LOW));
        service.handle(command(RiskLevel.CRITICAL, RiskLevel.LOW));

        verify(analyticsRepository, times(2)).save(existing);
        assertEquals(2, existing.getTrends().size());
        assertEquals(RiskLevel.CRITICAL, existing.getRiskLevel());
    }

    @Test
    void overallAndTrendRiskLevelsAreStoredIndependently() {
        when(analyticsRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        var trend = service.handle(command(RiskLevel.LOW, RiskLevel.CRITICAL));

        assertEquals(RiskLevel.CRITICAL, trend.getRiskLevel());
        assertEquals(RiskLevel.LOW, trend.getAnalytics().getRiskLevel());
    }

    @Test
    void asksForTheAlertOfTheNewTrendWithTheProvidedMessage() {
        when(analyticsRepository.findByOwnerId(ownerId)).thenReturn(Optional.empty());

        service.handle(command(RiskLevel.LOW, RiskLevel.HIGH));

        verify(riskAlertService).handle(new GenerateRiskAlertCommand(null, "mensaje de prueba"));
    }
}
