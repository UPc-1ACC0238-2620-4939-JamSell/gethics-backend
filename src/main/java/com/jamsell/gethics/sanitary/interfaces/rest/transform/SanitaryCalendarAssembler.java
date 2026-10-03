package com.jamsell.gethics.sanitary.interfaces.rest.transform;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryCalendarResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ScheduledEventResource;

import java.time.YearMonth;
import java.util.List;

public final class SanitaryCalendarAssembler {

    static final String NO_PENDING_ACTIVITIES = "No hay actividades pendientes.";

    private SanitaryCalendarAssembler() {
    }

    public static SanitaryCalendarResource toResource(YearMonth period, List<SanitaryEvent> events) {
        var items = events.stream().map(SanitaryCalendarAssembler::toResource).toList();
        return new SanitaryCalendarResource(period.getYear(), period.getMonthValue(), items,
                items.isEmpty() ? NO_PENDING_ACTIVITIES : null);
    }

    private static ScheduledEventResource toResource(SanitaryEvent event) {
        return new ScheduledEventResource(event.getId(), event.getClinicalHistory().getAnimalId(),
                event.getType(), event.getScheduledDate(), event.getDescription(), event.getStatus());
    }
}
