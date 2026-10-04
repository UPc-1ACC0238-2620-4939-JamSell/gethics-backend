package com.jamsell.gethics.veterinary.interfaces.rest.transform;

import com.jamsell.gethics.veterinary.domain.model.aggregates.VeterinaryAssignment;
import com.jamsell.gethics.veterinary.domain.model.commands.AssignClientCommand;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.AssignedClient;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.AssignClientResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.AssignedClientResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.AssignmentResource;

public final class VeterinaryAssignmentAssembler {

    private VeterinaryAssignmentAssembler() {
    }

    public static AssignClientCommand toCommand(AssignClientResource resource) {
        return new AssignClientCommand(resource.veterinarianId(), resource.clientId());
    }

    public static AssignmentResource toResource(VeterinaryAssignment entity) {
        return new AssignmentResource(entity.getId(), entity.getVeterinarianId(), entity.getClientId(),
                entity.getAssignedAt(), entity.getStatus());
    }

    public static AssignedClientResource toResource(AssignedClient client) {
        return new AssignedClientResource(client.assignmentId(), client.clientId(),
                client.location(), client.assignedAt());
    }
}
