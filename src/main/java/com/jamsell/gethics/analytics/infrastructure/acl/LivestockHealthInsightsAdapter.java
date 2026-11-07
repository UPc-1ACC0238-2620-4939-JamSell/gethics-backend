package com.jamsell.gethics.analytics.infrastructure.acl;

import com.jamsell.gethics.analytics.application.internal.outboundservices.HealthInsightsLookup;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Temporary adapter. Siempre reporta datos insuficientes: hoy no hay forma de resolver que animales pertenecen a un
 * ganadero ({@code Animal} solo tiene {@code farmId}, opcional, y Farm aun no esta implementado), y el historial
 * clinico de sanitary esta indexado por animalId sin relacion con un owner. Replace its body with a call to the
 * livestock/sanitary context facade once ambos exponan esa relacion.
 */
@Component
public class LivestockHealthInsightsAdapter implements HealthInsightsLookup {

    @Override
    public boolean hasSufficientData(UUID ownerId, LocalDate from, LocalDate to) {
        return false;
    }
}
