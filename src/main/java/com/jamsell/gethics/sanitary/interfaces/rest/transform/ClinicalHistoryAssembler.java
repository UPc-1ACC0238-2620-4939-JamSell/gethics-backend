package com.jamsell.gethics.sanitary.interfaces.rest.transform;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ClinicalHistoryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ClinicalHistoryResource;

import java.util.List;
import java.util.UUID;

public final class ClinicalHistoryAssembler {

    static final String NO_RECORDS = "Sin registros.";

    private ClinicalHistoryAssembler() {
    }

    public static ClinicalHistoryResource toResource(UUID animalId, List<SanitaryEvent> events) {
        var items = events.stream().map(ClinicalHistoryAssembler::toResource).toList();
        return new ClinicalHistoryResource(animalId, items, items.isEmpty() ? NO_RECORDS : null);
    }

    private static ClinicalHistoryEventResource toResource(SanitaryEvent event) {
        return new ClinicalHistoryEventResource(event.getId(), event.getType(), event.getStatus(),
                event.getOccurredAt(), event.getScheduledDate(), event.getDescription());
    }
}
