package com.jamsell.gethics.analytics.interfaces.scheduling;

import com.jamsell.gethics.analytics.domain.model.commands.AnalyzeLivestockTrendsCommand;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TrendAnalysisJobTest {

    @Test
    void onlyDelegatesToTheApplicationService() {
        var service = mock(TrendAnalysisCommandService.class);

        new TrendAnalysisJob(service).run();

        verify(service).handle(new AnalyzeLivestockTrendsCommand());
        verifyNoMoreInteractions(service);
    }

    @Test
    void cronComesFromConfigurationAndIsDisabledByDefault() throws Exception {
        var scheduled = TrendAnalysisJob.class.getMethod("run").getAnnotation(Scheduled.class);

        assertEquals("${gethics.analytics.analysis.cron:-}", scheduled.cron());
        assertEquals("-", Scheduled.CRON_DISABLED);
    }
}
