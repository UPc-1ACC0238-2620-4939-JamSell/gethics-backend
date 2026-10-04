package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.application.internal.commandservices.AlertDispatchCommandServiceImpl;
import com.jamsell.gethics.analytics.application.internal.commandservices.LivestockTrendCommandServiceImpl;
import com.jamsell.gethics.analytics.application.internal.commandservices.RiskAlertCommandServiceImpl;
import com.jamsell.gethics.analytics.application.internal.commandservices.TrendAnalysisCommandServiceImpl;
import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertDeliveryException;
import com.jamsell.gethics.analytics.application.internal.outboundservices.AlertNotification;
import com.jamsell.gethics.analytics.application.internal.outboundservices.PushNotificationService;
import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.commands.AnalyzeLivestockTrendsCommand;
import com.jamsell.gethics.analytics.domain.model.commands.DispatchPendingAlertsCommand;
import com.jamsell.gethics.analytics.domain.model.commands.GenerateRiskAlertCommand;
import com.jamsell.gethics.analytics.domain.model.commands.RegisterClassifiedTrendCommand;
import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.services.AlertRiskPolicy;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisService;
import com.jamsell.gethics.analytics.domain.services.TrendDetector;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mecanismo completo (servicios de aplicacion + repositorios reales sobre PostgreSQL) con un push de prueba. Los niveles
 * que generan alerta aqui (HIGH) son un valor de prueba, no una decision de negocio. No prueba deteccion de anomalias:
 * el clasificador no existe. Cada test hace rollback y solo mira lo que el propio test sembro.
 * <p>
 * Los tests del analisis periodico (US-21) usan un detector que existe SOLO aqui: devuelve lo que cada test le indica,
 * no detecta nada.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AlertFlowTest.TestBeans.class, TrendAnalysisCommandServiceImpl.class, LivestockTrendCommandServiceImpl.class,
        RiskAlertCommandServiceImpl.class, AlertDispatchCommandServiceImpl.class, AnalyticsRepositoryImpl.class, LivestockTrendRepositoryImpl.class,
        AlertRepositoryImpl.class})
class AlertFlowTest {

    private static final DispatchPendingAlertsCommand DISPATCH = new DispatchPendingAlertsCommand();
    private static final AnalyzeLivestockTrendsCommand ANALYZE = new AnalyzeLivestockTrendsCommand();

    /** Detector de TEST: devuelve las tendencias que el test le indique para la proxima ejecucion. */
    static class ScriptedDetector implements TrendDetector {
        List<RegisterClassifiedTrendCommand> next = List.of();

        @Override
        public List<RegisterClassifiedTrendCommand> detect() {
            return next;
        }
    }

    static class RecordingPush implements PushNotificationService {
        final List<AlertNotification> sent = new ArrayList<>();
        final Set<UUID> failingOwners = new HashSet<>();

        @Override
        public void send(AlertNotification n) {
            if (failingOwners.contains(n.ownerId())) {
                throw new AlertDeliveryException("proveedor caido");
            }
            sent.add(n);
        }

        long sentTo(UUID ownerId) {
            return sent.stream().filter(n -> n.ownerId().equals(ownerId)).count();
        }
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        ScriptedDetector detector() {
            return new ScriptedDetector();
        }

        @Bean
        RecordingPush push() {
            return new RecordingPush();
        }

        @Bean
        TrendAnalysisService trendAnalysisService() {
            return new TrendAnalysisService();
        }

        @Bean
        AlertRiskPolicy alertRiskPolicy() {
            return level -> level == RiskLevel.HIGH;
        }
    }

    @Autowired
    LivestockTrendCommandServiceImpl trendService;
    @Autowired
    RiskAlertCommandServiceImpl riskAlertService;
    @Autowired
    AlertDispatchCommandServiceImpl dispatchService;
    @Autowired
    TrendAnalysisCommandServiceImpl analysis;
    @Autowired
    ScriptedDetector detector;
    @Autowired
    RecordingPush push;
    @Autowired
    EntityManager em;

    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void detectorFindsNothingUnlessTheTestSaysOtherwise() {
        detector.next = List.of();
    }

    private RegisterClassifiedTrendCommand classified(RiskLevel overall, RiskLevel trendLevel) {
        return new RegisterClassifiedTrendCommand(ownerId, overall, TrendType.SANITARY, trendLevel, "descripcion",
                Instant.parse("2026-10-01T10:00:00Z"), "mensaje de prueba");
    }

    private List<Alert> alertsOfOwner() {
        em.clear();
        return em.createQuery("select a from Alert a where a.ownerId = :owner order by a.createdAt", Alert.class)
                .setParameter("owner", ownerId).getResultList();
    }

    private long analyticsOfOwner() {
        return em.createQuery("select count(a) from Analytics a where a.ownerId = :owner", Long.class)
                .setParameter("owner", ownerId).getSingleResult();
    }

    @Test
    void alertingLevelCreatesAPendingAlertForTheOwner() {
        var trend = trendService.handle(classified(RiskLevel.LOW, RiskLevel.HIGH));

        var alerts = alertsOfOwner();
        assertEquals(1, alerts.size());
        assertEquals(AlertStatus.PENDING, alerts.getFirst().getStatus());
        assertEquals("mensaje de prueba", alerts.getFirst().getMessage());
        assertEquals(trend.getId(), alerts.getFirst().getTrendId());
    }

    @Test
    void nonAlertingLevelCreatesTheTrendButNoAlert() {
        var trend = trendService.handle(classified(RiskLevel.HIGH, RiskLevel.MEDIUM));

        assertNotNull(trend.getId());
        assertTrue(alertsOfOwner().isEmpty());
    }

    @Test
    void ownerKeepsASingleAnalyticsAcrossRegistrations() {
        trendService.handle(classified(RiskLevel.LOW, RiskLevel.LOW));
        trendService.handle(classified(RiskLevel.MEDIUM, RiskLevel.LOW));

        assertEquals(1L, analyticsOfOwner());
        assertEquals(2L, em.createQuery("select count(t) from LivestockTrend t where t.analytics.ownerId = :owner", Long.class)
                .setParameter("owner", ownerId).getSingleResult());
    }

    @Test
    void alreadyProcessedTrendDoesNotGetASecondInitialAlert() {
        var trend = trendService.handle(classified(RiskLevel.LOW, RiskLevel.HIGH));

        var again = riskAlertService.handle(new GenerateRiskAlertCommand(trend.getId(), "otra vez"));

        assertTrue(again.isEmpty());
        assertEquals(1, alertsOfOwner().size());
    }

    @Test
    void pendingAlertIsPushedAndMarkedSentAndNeverResent() {
        trendService.handle(classified(RiskLevel.LOW, RiskLevel.HIGH));

        dispatchService.handle(DISPATCH);

        assertEquals(1, push.sentTo(ownerId));
        assertEquals(AlertStatus.SENT, alertsOfOwner().getFirst().getStatus());

        dispatchService.handle(DISPATCH);

        assertEquals(1, push.sentTo(ownerId));
    }

    @Test
    void pushFailureKeepsTheAlertPendingAndALaterRunSendsIt() {
        trendService.handle(classified(RiskLevel.LOW, RiskLevel.HIGH));
        push.failingOwners.add(ownerId);

        dispatchService.handle(DISPATCH);

        assertEquals(0, push.sentTo(ownerId));
        assertEquals(AlertStatus.PENDING, alertsOfOwner().getFirst().getStatus());

        push.failingOwners.clear();
        dispatchService.handle(DISPATCH);

        assertEquals(1, push.sentTo(ownerId));
        assertEquals(AlertStatus.SENT, alertsOfOwner().getFirst().getStatus());
    }

    private RegisterClassifiedTrendCommand detected(RiskLevel trendLevel) {
        return new RegisterClassifiedTrendCommand(ownerId, RiskLevel.LOW, TrendType.SANITARY, trendLevel,
                "tendencia de prueba", Instant.parse("2026-10-01T10:00:00Z"), "mensaje de prueba");
    }

    private List<LivestockTrend> trendsOfOwner() {
        em.clear();
        return em.createQuery("select t from LivestockTrend t where t.analytics.ownerId = :owner", LivestockTrend.class)
                .setParameter("owner", ownerId).getResultList();
    }

    private long count(String entity) {
        return em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
    }

    // ---- Analisis periodico (US-21) ----

    /** Escenario 2: indicadores normales -> el analisis periodico no genera tendencias, alertas ni push. */
    @Test
    void normalIndicatorsProduceNoTrendNoAlertAndNoPush() {
        var trendsBefore = count("LivestockTrend");
        var alertsBefore = count("Alert");
        var pushesBefore = push.sent.size();
        // El despacho es global: solo podria enviar alertas PENDING que ya existieran antes (ninguna creada ahora).
        var pendingBefore = em.createQuery("select count(a) from Alert a where a.status = :status", Long.class)
                .setParameter("status", AlertStatus.PENDING).getSingleResult();

        analysis.handle(ANALYZE);

        assertEquals(trendsBefore, count("LivestockTrend"));
        assertEquals(alertsBefore, count("Alert"));
        assertEquals(pushesBefore + pendingBefore, push.sent.size());
    }

    /** Escenario 1 (mecanismo): anomalia con nivel alertable -> tendencia, una Alert SENT y un unico push. */
    @Test
    void alertableTrendIsPersistedAlertedAndPushedExactlyOnce() {
        detector.next = List.of(detected(RiskLevel.HIGH));

        analysis.handle(ANALYZE);

        var trends = trendsOfOwner();
        assertEquals(1, trends.size());
        var alerts = alertsOfOwner();
        assertEquals(1, alerts.size());
        assertEquals(AlertStatus.SENT, alerts.getFirst().getStatus());
        assertEquals(trends.getFirst().getId(), alerts.getFirst().getTrendId());
        assertEquals(1, push.sentTo(ownerId));
        assertTrue(push.sent.contains(new AlertNotification(alerts.getFirst().getId(), ownerId, "mensaje de prueba")));
    }

    @Test
    void secondRunWithoutNewTrendsDoesNotResendASentAlert() {
        detector.next = List.of(detected(RiskLevel.HIGH));
        analysis.handle(ANALYZE);
        detector.next = List.of();

        analysis.handle(ANALYZE);

        assertEquals(1, trendsOfOwner().size());
        assertEquals(1, alertsOfOwner().size());
        assertEquals(AlertStatus.SENT, alertsOfOwner().getFirst().getStatus());
        assertEquals(1, push.sentTo(ownerId));
    }

    @Test
    void nonAlertableTrendIsPersistedWithoutAlertOrPush() {
        detector.next = List.of(detected(RiskLevel.LOW));

        analysis.handle(ANALYZE);

        assertEquals(1, trendsOfOwner().size());
        assertTrue(alertsOfOwner().isEmpty());
        assertEquals(0, push.sentTo(ownerId));
    }

    @Test
    void analysisPushFailureKeepsTheAlertPendingAndALaterRunSendsIt() {
        detector.next = List.of(detected(RiskLevel.HIGH));
        push.failingOwners.add(ownerId);

        analysis.handle(ANALYZE);

        assertEquals(0, push.sentTo(ownerId));
        assertEquals(AlertStatus.PENDING, alertsOfOwner().getFirst().getStatus());

        detector.next = List.of();
        push.failingOwners.clear();
        analysis.handle(ANALYZE);

        assertEquals(1, push.sentTo(ownerId));
        assertEquals(1, alertsOfOwner().size());
        assertEquals(AlertStatus.SENT, alertsOfOwner().getFirst().getStatus());
    }
}
