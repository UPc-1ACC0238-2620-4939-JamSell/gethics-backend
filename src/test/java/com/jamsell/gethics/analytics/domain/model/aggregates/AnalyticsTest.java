package com.jamsell.gethics.analytics.domain.model.aggregates;

import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsTest {

    private final UUID ownerId = UUID.randomUUID();
    private final Instant detectedAt = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void startsWithOwnerOverallRiskAndTimestamps() {
        var analytics = new Analytics(ownerId, RiskLevel.LOW);

        assertEquals(ownerId, analytics.getOwnerId());
        assertEquals(RiskLevel.LOW, analytics.getRiskLevel());
        assertNotNull(analytics.getCreatedAt());
        assertNotNull(analytics.getLastAnalysisAt());
        assertTrue(analytics.getTrends().isEmpty());
    }

    @Test
    void registeredTrendBelongsToTheAnalyticsAndKeepsItsOwnRiskLevel() {
        var analytics = new Analytics(ownerId, RiskLevel.LOW);

        var trend = analytics.registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "descripcion", detectedAt, RiskLevel.MEDIUM);

        assertEquals(1, analytics.getTrends().size());
        assertSame(trend, analytics.getTrends().get(0));
        assertSame(analytics, trend.getAnalytics());
        assertEquals(TrendType.SANITARY, trend.getType());
        assertEquals("descripcion", trend.getDescription());
        assertEquals(detectedAt, trend.getDetectedAt());
    }

    @Test
    void overallAndTrendRiskLevelsAreIndependent() {
        var analytics = new Analytics(ownerId, RiskLevel.LOW);

        var trend = analytics.registerTrend(TrendType.FINANCIAL, RiskLevel.CRITICAL, "d", detectedAt, RiskLevel.LOW);

        assertEquals(RiskLevel.CRITICAL, trend.getRiskLevel());
        assertEquals(RiskLevel.LOW, analytics.getRiskLevel());

        var other = analytics.registerTrend(TrendType.COMBINED, RiskLevel.LOW, "d", detectedAt, RiskLevel.HIGH);

        assertEquals(RiskLevel.LOW, other.getRiskLevel());
        assertEquals(RiskLevel.HIGH, analytics.getRiskLevel());
    }

    @Test
    void overallRiskIsTheReceivedOneNotDerivedFromTheLastTrend() {
        var analytics = new Analytics(ownerId, RiskLevel.MEDIUM);

        analytics.registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "d", detectedAt, RiskLevel.MEDIUM);

        assertEquals(RiskLevel.MEDIUM, analytics.getRiskLevel());
    }

    @Test
    void registeringATrendRefreshesLastAnalysisAt() throws InterruptedException {
        var analytics = new Analytics(ownerId, RiskLevel.LOW);
        var before = analytics.getLastAnalysisAt();
        Thread.sleep(2);

        analytics.registerTrend(TrendType.SANITARY, RiskLevel.LOW, "d", detectedAt, RiskLevel.LOW);

        assertTrue(analytics.getLastAnalysisAt().isAfter(before));
    }

    @Test
    void requiredFieldsAreEnforced() {
        assertThrows(NullPointerException.class, () -> new Analytics(null, RiskLevel.LOW));
        assertThrows(NullPointerException.class, () -> new Analytics(ownerId, null));
        var analytics = new Analytics(ownerId, RiskLevel.LOW);
        assertThrows(NullPointerException.class,
                () -> analytics.registerTrend(null, RiskLevel.LOW, "d", detectedAt, RiskLevel.LOW));
        assertThrows(NullPointerException.class,
                () -> analytics.registerTrend(TrendType.SANITARY, null, "d", detectedAt, RiskLevel.LOW));
        assertThrows(NullPointerException.class,
                () -> analytics.registerTrend(TrendType.SANITARY, RiskLevel.LOW, "d", null, RiskLevel.LOW));
        // description es NOT NULL en el informe
        assertThrows(NullPointerException.class,
                () -> analytics.registerTrend(TrendType.SANITARY, RiskLevel.LOW, null, detectedAt, RiskLevel.LOW));
        assertTrue(analytics.getTrends().isEmpty());
    }

    @Test
    void descriptionHasNoMaximumLength() {
        var longText = "x".repeat(100_000);

        var trend = new Analytics(ownerId, RiskLevel.LOW)
                .registerTrend(TrendType.SANITARY, RiskLevel.LOW, longText, detectedAt, RiskLevel.LOW);

        assertEquals(longText, trend.getDescription());
    }
}
