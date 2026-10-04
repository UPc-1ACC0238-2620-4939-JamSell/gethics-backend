package com.jamsell.gethics.veterinary.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientLocationLookup;
import com.jamsell.gethics.veterinary.domain.model.aggregates.VeterinaryAssignment;
import com.jamsell.gethics.veterinary.domain.model.queries.GetAssignedClientsQuery;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VeterinaryAssignmentQueryServiceImplTest {

    private final VeterinaryAssignmentRepository repository = mock(VeterinaryAssignmentRepository.class);
    private final ClientLocationLookup locationLookup = mock(ClientLocationLookup.class);
    private final VeterinaryAssignmentQueryServiceImpl service =
            new VeterinaryAssignmentQueryServiceImpl(repository, locationLookup);

    @Test
    void returnsEmptyListWhenNoClientsAssigned() {
        var veterinarianId = UUID.randomUUID();
        when(repository.findByVeterinarianIdAndStatus(veterinarianId, VeterinaryAssignmentStatus.ACTIVE))
                .thenReturn(List.of());

        var result = service.handle(new GetAssignedClientsQuery(veterinarianId));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsAssignedClientsWithLocation() {
        var veterinarianId = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var assignment = new VeterinaryAssignment(veterinarianId, clientId);
        when(repository.findByVeterinarianIdAndStatus(veterinarianId, VeterinaryAssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(locationLookup.findLocationByClientId(clientId)).thenReturn(Optional.of("Jauja, Junin"));

        var result = service.handle(new GetAssignedClientsQuery(veterinarianId));

        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).clientId());
        assertEquals("Jauja, Junin", result.get(0).location());
    }
}

