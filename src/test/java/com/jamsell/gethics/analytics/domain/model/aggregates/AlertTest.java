package com.jamsell.gethics.analytics.domain.model.aggregates;

import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    private final UUID ownerId = UUID.randomUUID();
    private final LivestockTrend trend = new Analytics(ownerId, RiskLevel.LOW)
            .registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "d", Instant.now(), RiskLevel.LOW);

    @Test
    void newAlertIsPendingAndReferencesOwnerAndTrend() {
        var alert = Alert.create(ownerId, trend, "mensaje de prueba");

        assertEquals(AlertStatus.PENDING, alert.getStatus());
        assertEquals(ownerId, alert.getOwnerId());
        assertSame(trend, alert.getTrend());
        assertEquals("mensaje de prueba", alert.getMessage());
        assertNotNull(alert.getCreatedAt());
    }

    @Test
    void markSentMovesPendingToSent() {
        var alert = Alert.create(ownerId, trend, "m");

        alert.markSent();

        assertEquals(AlertStatus.SENT, alert.getStatus());
    }

    @Test
    void sentAlertCannotBeMarkedSentAgain() {
        var alert = Alert.create(ownerId, trend, "m");
        alert.markSent();

        assertThrows(IllegalStateException.class, alert::markSent);
    }

    @Test
    void messageIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> Alert.create(ownerId, trend, null));
        assertThrows(IllegalArgumentException.class, () -> Alert.create(ownerId, trend, "  "));
    }

    @Test
    void messageHasNoMaximumLength() {
        var longText = "x".repeat(100_000);

        assertEquals(longText, Alert.create(ownerId, trend, longText).getMessage());
    }

    @Test
    void ownerAndTrendAreRequired() {
        assertThrows(NullPointerException.class, () -> Alert.create(null, trend, "m"));
        assertThrows(NullPointerException.class, () -> Alert.create(ownerId, null, "m"));
    }

    @Test
    void sameTrendMayHaveSeveralAlerts() {
        var first = Alert.create(ownerId, trend, "uno");
        var second = Alert.create(ownerId, trend, "dos");

        assertSame(first.getTrend(), second.getTrend());
        assertNotSame(first, second);
    }
}
