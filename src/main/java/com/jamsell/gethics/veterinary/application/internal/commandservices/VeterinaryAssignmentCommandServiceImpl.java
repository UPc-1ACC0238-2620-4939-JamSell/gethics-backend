package com.jamsell.gethics.veterinary.application.internal.commandservices;

import com.jamsell.gethics.veterinary.domain.model.aggregates.VeterinaryAssignment;
import com.jamsell.gethics.veterinary.domain.model.commands.AssignClientCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.UnassignClientCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.AssignmentNotFoundException;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryAssignmentCommandService;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeterinaryAssignmentCommandServiceImpl implements VeterinaryAssignmentCommandService {

    private final VeterinaryAssignmentRepository assignmentRepository;

    public VeterinaryAssignmentCommandServiceImpl(VeterinaryAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional
    public VeterinaryAssignment handle(AssignClientCommand command) {
        var alreadyAssigned = assignmentRepository.existsByVeterinarianIdAndClientIdAndStatus(
                command.veterinarianId(), command.clientId(), VeterinaryAssignmentStatus.ACTIVE);
        if (alreadyAssigned) {
            throw new IllegalStateException("Client is already assigned to this veterinarian");
        }
        var assignment = new VeterinaryAssignment(command.veterinarianId(), command.clientId());
        return assignmentRepository.save(assignment);
    }

    /**
     * US-34: la asignacion pasa a INACTIVE (no se borra): el cliente deja de aparecer en la lista activa y queda el
     * rastro de la relacion. Una asignacion inexistente o ya desvinculada es "no encontrada" (404).
     */
    @Override
    @Transactional
    public void handle(UnassignClientCommand command) {
        var assignment = assignmentRepository.findById(command.assignmentId())
                .filter(VeterinaryAssignment::isActive)
                .orElseThrow(() -> new AssignmentNotFoundException(command.assignmentId()));
        assignment.deactivate();
        assignmentRepository.save(assignment);
    }
}
