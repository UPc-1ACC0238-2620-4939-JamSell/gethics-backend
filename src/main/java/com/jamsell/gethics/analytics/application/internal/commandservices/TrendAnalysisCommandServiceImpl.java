package com.jamsell.gethics.analytics.application.internal.commandservices;

import com.jamsell.gethics.analytics.domain.model.commands.AnalyzeLivestockTrendsCommand;
import com.jamsell.gethics.analytics.domain.model.commands.DispatchPendingAlertsCommand;
import com.jamsell.gethics.analytics.domain.services.AlertDispatchCommandService;
import com.jamsell.gethics.analytics.domain.services.LivestockTrendCommandService;
import com.jamsell.gethics.analytics.domain.services.TrendAnalysisCommandService;
import com.jamsell.gethics.analytics.domain.services.TrendDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Analisis periodico: cada tendencia que devuelven los {@link TrendDetector} se registra con
 * {@link LivestockTrendCommandService} (que aplica la {@code AlertRiskPolicy} y crea la Alert si corresponde) y al final se
 * despachan las alertas PENDING, incluidas las que fallaron en ejecuciones anteriores.
 * <p>
 * Funciona con cero detectores (el estado actual del proyecto: el algoritmo no esta definido): no registra tendencias,
 * lo deja en el log y aun asi despacha las alertas pendientes. El despacho se ejecuta tambien si un detector falla.
 * Deliberadamente NO es {@code @Transactional}: cada registro y cada envio confirman por separado.
 */
@Slf4j
@Service
public class TrendAnalysisCommandServiceImpl implements TrendAnalysisCommandService {

    private final ObjectProvider<TrendDetector> detectors;
    private final LivestockTrendCommandService trendService;
    private final AlertDispatchCommandService dispatchService;

    public TrendAnalysisCommandServiceImpl(ObjectProvider<TrendDetector> detectors, LivestockTrendCommandService trendService,
                                           AlertDispatchCommandService dispatchService) {
        this.detectors = detectors;
        this.trendService = trendService;
        this.dispatchService = dispatchService;
    }

    @Override
    public void handle(AnalyzeLivestockTrendsCommand command) {
        try {
            var configured = detectors.orderedStream().toList();
            if (configured.isEmpty()) {
                log.warn("Analisis de tendencias: no hay ningun TrendDetector configurado (algoritmo de deteccion pendiente "
                        + "de definicion); no se registran tendencias");
                return;
            }
            int registered = 0;
            for (var detector : configured) {
                for (var trend : detector.detect()) {
                    trendService.handle(trend);
                    registered++;
                }
            }
            log.info("Analisis de tendencias: {} detectores, {} tendencias registradas", configured.size(), registered);
        } finally {
            dispatchService.handle(new DispatchPendingAlertsCommand());
        }
    }
}
