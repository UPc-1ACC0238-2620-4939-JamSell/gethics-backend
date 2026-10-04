package com.jamsell.gethics.veterinary.application.internal.queryservices;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientLocationLookup;
import com.jamsell.gethics.veterinary.domain.model.queries.GetAssignedClientsQuery;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.AssignedClient;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryAssignmentQueryService;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeterinaryAssignmentQueryServiceImpl implements VeterinaryAssignmentQueryService {

    private final VeterinaryAssignmentRepository assignmentRepository;
    private final ClientLocationLookup clientLocationLookup;

    public VeterinaryAssignmentQueryServiceImpl(VeterinaryAssignmentRepository assignmentRepository,
                                                ClientLocationLookup clientLocationLookup) {
        this.assignmentRepository = assignmentRepository;
        this.clientLocationLookup = clientLocationLookup;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedClient> handle(GetAssignedClientsQuery query) {
        return assignmentRepository
                .findByVeterinarianIdAndStatus(query.veterinarianId(), VeterinaryAssignmentStatus.ACTIVE)
                .stream()
                .map(assignment -> new AssignedClient(
                        assignment.getId(),
                        assignment.getClientId(),
                        clientLocationLookup.findLocationByClientId(assignment.getClientId()).orElse(null),
                        assignment.getAssignedAt()))
                .toList();
    }
}