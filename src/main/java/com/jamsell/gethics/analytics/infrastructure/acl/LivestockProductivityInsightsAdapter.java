package com.jamsell.gethics.analytics.infrastructure.acl;

import com.jamsell.gethics.analytics.application.internal.outboundservices.ProductivityInsightsLookup;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Temporary adapter. Siempre reporta datos insuficientes: no existe hoy un modelo de datos de productividad mas
 * alla del peso inicial al registrar el animal (sin historial de peso ni de produccion), y el analisis de
 * tendencias (bounded context analytics) funciona con cero TrendDetector registrados, por lo que
 * LivestockTrend esta siempre vacio. Replace its body with a call to the livestock/analytics context facade
 * once alguno de los dos exponga datos reales de productividad.
 */
@Component
public class LivestockProductivityInsightsAdapter implements ProductivityInsightsLookup {

    @Override
    public boolean hasSufficientData(UUID ownerId, LocalDate from, LocalDate to) {
        return false;
    }
}
