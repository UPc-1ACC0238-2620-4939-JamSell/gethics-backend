package com.jamsell.gethics.sanitary.interfaces.rest.transform;

import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.CompleteSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.RegisterSanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SanitaryEventResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.ScheduleSanitaryEventResource;

import java.util.UUID;

public final class SanitaryEventAssembler {

    private SanitaryEventAssembler() {
    }

    public static RegisterSanitaryEventCommand toCommand(UUID animalId, RegisterSanitaryEventResource resource) {
        return new RegisterSanitaryEventCommand(animalId, resource.type(), resource.occurredAt(), resource.description());
    }

    public static ScheduleSanitaryEventCommand toCommand(UUID animalId, ScheduleSanitaryEventResource resource) {
        return new ScheduleSanitaryEventCommand(animalId, resource.type(), resource.scheduledDate(), resource.description());
    }

    public static CompleteScheduledEventCommand toCommand(UUID animalId, UUID eventId, CompleteSanitaryEventResource resource) {
        return new CompleteScheduledEventCommand(animalId, eventId, resource.occurredAt(), resource.description());
    }

    public static SanitaryEventResource toResource(SanitaryEvent event) {
        var history = event.getClinicalHistory();
        return new SanitaryEventResource(event.getId(), history.getId(), history.getAnimalId(), event.getType(),
                event.getOccurredAt(), event.getScheduledDate(), event.getDescription(), event.getStatus());
    }
}
