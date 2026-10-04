package com.jamsell.gethics.analytics;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import com.jamsell.gethics.analytics.domain.repositories.AnalyticsRepository;
import com.jamsell.gethics.analytics.domain.services.TrendDetector;
import com.jamsell.gethics.analytics.interfaces.scheduling.TrendAnalysisJob;
import com.jamsell.gethics.sanitary.interfaces.scheduling.VaccinationReminderJob;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aplicacion completa en su estado productivo real: sin ningun TrendDetector, sin configurar el cron del analisis ni la
 * politica de alertas, y con el adapter push simulado (log). Misma configuracion que {@code GethicsApplicationTests}, de
 * modo que reutiliza su contexto. Cada test hace rollback.
 */
@SpringBootTest
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class TrendAnalysisWithoutDetectorTest {

    @Autowired
    ApplicationContext context;
    @Autowired
    TrendAnalysisJob job;
    @Autowired
    AnalyticsRepository analyticsRepository;
    @Autowired
    AlertRepository alertRepository;
    @Autowired
    EntityManager em;

    private long count(String entity) {
        return em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult();
    }

    private boolean isScheduled(Class<?> job) {
        return context.getBeansOfType(ScheduledTaskHolder.class).values().stream()
                .flatMap(holder -> holder.getScheduledTasks().stream())
                .anyMatch(task -> task.toString().contains(job.getName()));
    }

    @Test
    void applicationStartsWithoutDetectorsAndWithTheAnalysisCronDisabled() {
        assertEquals(0, context.getBeanNamesForType(TrendDetector.class).length);
        assertFalse(context.getEnvironment().containsProperty("gethics.analytics.analysis.cron"));
        assertFalse(isScheduled(TrendAnalysisJob.class));
        // Control: el mismo mecanismo si ve un job con cron configurado.
        assertTrue(isScheduled(VaccinationReminderJob.class));
    }

    @Test
    void analysisWithoutDetectorCreatesNothingButDispatchesPendingAlertsFromPreviousRuns(CapturedOutput output) {
        // Alerta PENDING "de una ejecucion anterior" (por ejemplo, un push que fallo).
        var ownerId = UUID.randomUUID();
        var analytics = new Analytics(ownerId, RiskLevel.LOW);
        analytics.registerTrend(TrendType.FINANCIAL, RiskLevel.HIGH, "tendencia previa",
                Instant.parse("2026-10-01T10:00:00Z"), RiskLevel.LOW);
        var trend = analyticsRepository.save(analytics).getTrends().getLast();
        var pending = alertRepository.save(Alert.create(ownerId, trend, "alerta previa"));
        var trendsBefore = count("LivestockTrend");
        var alertsBefore = count("Alert");

        job.run();

        assertEquals(trendsBefore, count("LivestockTrend"));
        assertEquals(alertsBefore, count("Alert"));
        assertTrue(output.getOut().contains("no hay ningun TrendDetector configurado"));
        assertTrue(output.getOut().contains("alerta=" + pending.getId()));
        em.clear();
        assertEquals(AlertStatus.SENT, em.find(Alert.class, pending.getId()).getStatus());
    }
}
