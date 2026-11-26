package com.jamsell.gethics.sanitary.interfaces.rest.transform;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.UpcomingSanitaryEvents;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.UpcomingSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.UpcomingSanitaryEventsResource;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class UpcomingSanitaryEventsAssembler {

    static final String NO_PENDING_ALERTS = "No hay alertas pendientes.";

    private UpcomingSanitaryEventsAssembler() {
    }

    public static UpcomingSanitaryEventsResource toResource(UpcomingSanitaryEvents upcoming) {
        var items = upcoming.events().stream().map(event -> toResource(event, upcoming.today())).toList();
        return new UpcomingSanitaryEventsResource(upcoming.today(), upcoming.days(), items,
                items.isEmpty() ? NO_PENDING_ALERTS : null);
    }

    private static UpcomingSanitaryEventResource toResource(SanitaryEvent event, LocalDate today) {
        var daysUntil = ChronoUnit.DAYS.between(today, event.getScheduledDate());
        return new UpcomingSanitaryEventResource(event.getId(), event.getClinicalHistory().getAnimalId(), event.getType(),
                event.getScheduledDate(), event.getDescription(), daysUntil, daysUntil < 0);
    }
}
