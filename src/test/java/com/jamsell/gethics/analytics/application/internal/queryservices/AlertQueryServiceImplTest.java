package com.jamsell.gethics.analytics.application.internal.queryservices;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.queries.GetAlertsByOwnerQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertQueryServiceImplTest {

    private final AlertRepository repository = mock(AlertRepository.class);
    private final AlertQueryServiceImpl service = new AlertQueryServiceImpl(repository);
    private final UUID ownerId = UUID.randomUUID();

    @Test
    void returnsTheAlertsOfThatOwner() {
        var analytics = new Analytics(ownerId, RiskLevel.LOW);
        var trend = analytics.registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "descripcion",
                Instant.parse("2026-10-01T10:00:00Z"), RiskLevel.LOW);
        var alerts = List.of(Alert.create(ownerId, trend, "mensaje"));
        when(repository.findByOwnerId(ownerId)).thenReturn(alerts);

        assertEquals(alerts, service.handle(new GetAlertsByOwnerQuery(ownerId)));
    }

    @Test
    void returnsAnEmptyListWhenTheOwnerHasNoAlerts() {
        when(repository.findByOwnerId(ownerId)).thenReturn(List.of());

        assertTrue(service.handle(new GetAlertsByOwnerQuery(ownerId)).isEmpty());
    }
}
