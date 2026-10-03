package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import com.jamsell.gethics.analytics.domain.repositories.AnalyticsRepository;
import com.jamsell.gethics.analytics.domain.repositories.LivestockTrendRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada test
 * hace rollback y usa ownerId aleatorios: solo verifica lo que el propio test sembro, sin borrar datos ajenos.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AnalyticsRepositoryImpl.class, LivestockTrendRepositoryImpl.class, AlertRepositoryImpl.class})
class AnalyticsPersistenceTest {

    private final Instant detectedAt = Instant.parse("2026-10-01T10:00:00Z");

    @Autowired
    AnalyticsRepository analyticsRepository;
    @Autowired
    LivestockTrendRepository trendRepository;
    @Autowired
    AlertRepository alertRepository;
    @Autowired
    EntityManager em;

    private Analytics analyticsWithTrend() {
        var analytics = new Analytics(UUID.randomUUID(), RiskLevel.LOW);
        analytics.registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "descripcion", detectedAt, RiskLevel.MEDIUM);
        return analyticsRepository.save(analytics);
    }

    @Test
    void analyticsPersistsWithItsOwnerAndTrends() {
        var analytics = new Analytics(UUID.randomUUID(), RiskLevel.LOW);
        analytics.registerTrend(TrendType.SANITARY, RiskLevel.HIGH, "uno", detectedAt, RiskLevel.MEDIUM);
        analytics.registerTrend(TrendType.FINANCIAL, RiskLevel.LOW, "dos", detectedAt.plusSeconds(60), RiskLevel.CRITICAL);
        analyticsRepository.save(analytics);
        em.clear();

        var reloaded = analyticsRepository.findByOwnerId(analytics.getOwnerId()).orElseThrow();

        assertEquals(analytics.getId(), reloaded.getId());
        assertEquals(RiskLevel.CRITICAL, reloaded.getRiskLevel());
        assertEquals(2, reloaded.getTrends().size());
        var first = reloaded.getTrends().stream().filter(t -> "uno".equals(t.getDescription())).findFirst().orElseThrow();
        assertEquals(TrendType.SANITARY, first.getType());
        assertEquals(RiskLevel.HIGH, first.getRiskLevel());
        assertEquals(detectedAt, first.getDetectedAt());
        assertEquals(reloaded.getId(), first.getAnalyticsId());
    }

    @Test
    void analyticsWithoutTrendsIsValid() {
        var analytics = analyticsRepository.save(new Analytics(UUID.randomUUID(), RiskLevel.LOW));
        em.clear();

        assertTrue(analyticsRepository.findByOwnerId(analytics.getOwnerId()).orElseThrow().getTrends().isEmpty());
    }

    @Test
    void unknownOwnerHasNoAnalytics() {
        assertTrue(analyticsRepository.findByOwnerId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void trendIsFoundById() {
        var trend = analyticsWithTrend().getTrends().getFirst();
        em.clear();

        var found = trendRepository.findById(trend.getId()).orElseThrow();

        assertEquals(RiskLevel.HIGH, found.getRiskLevel());
        assertTrue(trendRepository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void alertIsPersistedPendingAndTrendCanHaveSeveralAlerts() {
        var analytics = analyticsWithTrend();
        var trend = analytics.getTrends().getFirst();
        var first = alertRepository.save(Alert.create(analytics.getOwnerId(), trend, "uno"));
        var second = alertRepository.save(Alert.create(analytics.getOwnerId(), trend, "dos"));
        em.clear();

        assertNotEquals(first.getId(), second.getId());
        assertEquals(2L, em.createQuery("select count(a) from Alert a where a.trend.id = :id", Long.class)
                .setParameter("id", trend.getId()).getSingleResult());
        assertEquals(AlertStatus.PENDING, em.find(Alert.class, first.getId()).getStatus());
        assertTrue(alertRepository.existsByTrendId(trend.getId()));
        assertFalse(alertRepository.existsByTrendId(UUID.randomUUID()));
    }

    @Test
    void findPendingReturnsOnlyPendingAlerts() {
        var analytics = analyticsWithTrend();
        var trend = analytics.getTrends().getFirst();
        var pending = alertRepository.save(Alert.create(analytics.getOwnerId(), trend, "pendiente"));
        var sent = alertRepository.save(Alert.create(analytics.getOwnerId(), trend, "enviada"));
        sent.markSent();
        alertRepository.save(sent);
        em.clear();

        var ids = alertRepository.findPending().stream().map(Alert::getId).collect(Collectors.toSet());

        assertTrue(ids.contains(pending.getId()));
        assertFalse(ids.contains(sent.getId()));
        assertEquals(AlertStatus.SENT, em.find(Alert.class, sent.getId()).getStatus());
    }

    @Test
    void longDescriptionAndMessageRoundTripWithoutTruncation() {
        var longText = "tendencia ".repeat(5_000); // 50.000 caracteres: sin limite de longitud
        var analytics = new Analytics(UUID.randomUUID(), RiskLevel.LOW);
        var trend = analytics.registerTrend(TrendType.COMBINED, RiskLevel.HIGH, longText, detectedAt, RiskLevel.LOW);
        analyticsRepository.save(analytics);
        var alert = alertRepository.save(Alert.create(analytics.getOwnerId(), trend, longText));
        em.clear();

        assertEquals(longText, trendRepository.findById(trend.getId()).orElseThrow().getDescription());
        assertEquals(longText, em.find(Alert.class, alert.getId()).getMessage());
    }

    @Test
    void descriptionAndMessageAreTextNotNullAndTimestampsAreTimestampTz() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                select table_name, column_name, data_type, character_maximum_length, is_nullable
                from information_schema.columns
                where table_name in ('analytics', 'livestock_trends', 'alerts')""").getResultList();
        var columns = rows.stream().collect(Collectors.toMap(r -> r[0] + "." + r[1], r -> r));

        for (var textColumn : List.of("livestock_trends.description", "alerts.message")) {
            assertEquals("text", columns.get(textColumn)[2], textColumn);
            assertNull(columns.get(textColumn)[3], textColumn + " no debe tener longitud maxima");
            assertEquals("NO", columns.get(textColumn)[4], textColumn + " es NOT NULL");
        }
        for (var timestamp : List.of("analytics.created_at", "analytics.last_analysis_at",
                "livestock_trends.detected_at", "alerts.created_at")) {
            assertEquals("timestamp with time zone", columns.get(timestamp)[2], timestamp);
        }
    }

    @Test
    void foreignKeysExistOnlyWhereTheReportDefinesThem() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                select tc.table_name, kcu.column_name, ccu.table_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name and tc.constraint_schema = kcu.constraint_schema
                join information_schema.constraint_column_usage ccu
                  on tc.constraint_name = ccu.constraint_name and tc.constraint_schema = ccu.constraint_schema
                where tc.constraint_type = 'FOREIGN KEY' and tc.table_name in ('analytics', 'livestock_trends', 'alerts')""")
                .getResultList();

        var foreignKeys = rows.stream().map(r -> r[0] + "." + r[1] + "->" + r[2]).collect(Collectors.toSet());

        // analytics_id y trend_id son FK reales; owner_id (analytics y alerts) NO tiene FK hacia IAM.
        assertEquals(Set.of("livestock_trends.analytics_id->analytics", "alerts.trend_id->livestock_trends"), foreignKeys);
    }

    @Test
    void onlyAnalyticsOwnerIdIsUniqueAndAlertTrendIdIsNot() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                select tc.table_name, kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name and tc.constraint_schema = kcu.constraint_schema
                where tc.constraint_type = 'UNIQUE' and tc.table_name in ('analytics', 'livestock_trends', 'alerts')""")
                .getResultList();

        var unique = rows.stream().map(r -> r[0] + "." + r[1]).collect(Collectors.toSet());

        assertEquals(Set.of("analytics.owner_id"), unique);
    }

    // Debe ser el ultimo statement: la violacion deja abortada la transaccion de PostgreSQL.
    @Test
    void twoAnalyticsWithTheSameOwnerViolateTheUniqueConstraint() {
        var ownerId = UUID.randomUUID();
        analyticsRepository.save(new Analytics(ownerId, RiskLevel.LOW));

        assertThrows(DataIntegrityViolationException.class,
                () -> analyticsRepository.save(new Analytics(ownerId, RiskLevel.HIGH)));
    }
}
