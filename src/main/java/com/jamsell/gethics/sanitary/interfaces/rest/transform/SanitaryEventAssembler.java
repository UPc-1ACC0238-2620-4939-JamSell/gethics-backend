package com.jamsell.gethics.sanitary.interfaces.rest.transform;

import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.RegisterSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryEventResource;

import java.util.UUID;

public final class SanitaryEventAssembler {

    private SanitaryEventAssembler() {
    }

    public static RegisterSanitaryEventCommand toCommand(UUID animalId, RegisterSanitaryEventResource resource) {
        return new RegisterSanitaryEventCommand(animalId, resource.type(), resource.occurredAt(), resource.description());
    }

    public static SanitaryEventResource toResource(SanitaryEvent event) {
        var history = event.getClinicalHistory();
        return new SanitaryEventResource(event.getId(), history.getId(), history.getAnimalId(),
                event.getType(), event.getOccurredAt(), event.getDescription(), event.getStatus());
    }
}
