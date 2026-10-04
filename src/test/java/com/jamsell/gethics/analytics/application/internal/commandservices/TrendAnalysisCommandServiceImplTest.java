package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.domain.model.commands.AnalyzeLivestockTrendsCommand;
import com.jamsell.gethics.analytics.domain.model.commands.DispatchPendingAlertsCommand;
import com.jamsell.gethics.analytics.domain.model.commands.RegisterClassifiedTrendCommand;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.services.AlertDispatchCommandService;
import com.jamsell.gethics.analytics.domain.services.LivestockTrendCommandService;
import com.jamsell.gethics.analytics.domain.services.TrendDetector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TrendAnalysisCommandServiceImplTest {

    private static final AnalyzeLivestockTrendsCommand COMMAND = new AnalyzeLivestockTrendsCommand();

    private final LivestockTrendCommandService trendService = mock(LivestockTrendCommandService.class);
    private final AlertDispatchCommandService dispatchService = mock(AlertDispatchCommandService.class);

    /** Contenedor real de Spring con exactamente los detectores dados (ninguno = el estado productivo actual). */
    private TrendAnalysisCommandServiceImpl serviceWith(TrendDetector... detectors) {
        var beanFactory = new DefaultListableBeanFactory();
        for (int i = 0; i < detectors.length; i++) {
            beanFactory.registerSingleton("detector" + i, detectors[i]);
        }
        return new TrendAnalysisCommandServiceImpl(beanFactory.getBeanProvider(TrendDetector.class), trendService,
                dispatchService);
    }

    private static RegisterClassifiedTrendCommand trend(RiskLevel level) {
        return new RegisterClassifiedTrendCommand(UUID.randomUUID(), RiskLevel.LOW, TrendType.FINANCIAL, level,
                "tendencia de prueba", Instant.parse("2026-10-01T10:00:00Z"), "mensaje de prueba");
    }

    @Test
    void withoutDetectorsRegistersNothingButStillDispatchesPendingAlerts() {
        serviceWith().handle(COMMAND);

        verifyNoInteractions(trendService);
        verify(dispatchService).handle(new DispatchPendingAlertsCommand());
    }

    @Test
    void detectorWithNormalIndicatorsRegistersNothing() {
        serviceWith(List::of).handle(COMMAND);

        verifyNoInteractions(trendService);
        verify(dispatchService).handle(new DispatchPendingAlertsCommand());
    }

    @Test
    void everyDetectedTrendIsRegisteredBeforeDispatching() {
        var first = trend(RiskLevel.HIGH);
        var second = trend(RiskLevel.LOW);
        var third = trend(RiskLevel.MEDIUM);

        serviceWith(() -> List.of(first, second), () -> List.of(third)).handle(COMMAND);

        var order = inOrder(trendService, dispatchService);
        order.verify(trendService).handle(first);
        order.verify(trendService).handle(second);
        order.verify(trendService).handle(third);
        order.verify(dispatchService).handle(new DispatchPendingAlertsCommand());
        verifyNoMoreInteractions(trendService, dispatchService);
    }

    @Test
    void failingDetectorPropagatesButPendingAlertsAreStillDispatched() {
        TrendDetector failing = () -> {
            throw new IllegalStateException("detector caido");
        };

        assertThrows(IllegalStateException.class, () -> serviceWith(failing).handle(COMMAND));

        verify(trendService, never()).handle(any());
        verify(dispatchService).handle(new DispatchPendingAlertsCommand());
    }
}
