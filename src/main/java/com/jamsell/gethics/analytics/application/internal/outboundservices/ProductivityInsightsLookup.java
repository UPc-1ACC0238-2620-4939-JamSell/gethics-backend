package com.jamsell.gethics.analytics.application.internal.outboundservices;

import java.time.LocalDate;
import java.util.UUID;

public interface ProductivityInsightsLookup {

    boolean hasSufficientData(UUID ownerId, LocalDate from, LocalDate to);
}
