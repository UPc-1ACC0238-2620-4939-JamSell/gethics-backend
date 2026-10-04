package com.jamsell.gethics.analytics.interfaces.scheduling;

import com.jamsell.gethics.analytics.domain.model.commands.AnalyzeLivestockTrendsCommand;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Disparador periodico (adapter de entrada): solo delega en el servicio de aplicacion. La periodicidad no esta definida
 * por el negocio: sin {@code gethics.analytics.analysis.cron} queda deshabilitado ({@link Scheduled#CRON_DISABLED}).
 */
@Component
public class TrendAnalysisJob {

    static final String CRON = "${gethics.analytics.analysis.cron:" + Scheduled.CRON_DISABLED + "}";

    private final TrendAnalysisCommandService commandService;

    public TrendAnalysisJob(TrendAnalysisCommandService commandService) {
        this.commandService = commandService;
    }

    @Scheduled(cron = CRON)
    public void run() {
        commandService.handle(new AnalyzeLivestockTrendsCommand());
    }
}
