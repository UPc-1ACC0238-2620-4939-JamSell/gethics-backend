package com.jamsell.gethics.veterinary.application.internal.commandservices;

import com.jamsell.gethics.veterinary.domain.model.aggregates.VeterinaryAssignment;
import com.jamsell.gethics.veterinary.domain.model.commands.AssignClientCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.UnassignClientCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.AssignmentNotFoundException;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-34: desvincular conserva el rastro (INACTIVE) y es "no encontrado" si no hay asignacion activa. */
class VeterinaryAssignmentCommandServiceImplTest {

    private final VeterinaryAssignmentRepository repository = mock(VeterinaryAssignmentRepository.class);
    private final VeterinaryAssignmentCommandServiceImpl service = new VeterinaryAssignmentCommandServiceImpl(repository);
    private final UUID vet = UUID.randomUUID();
    private final UUID client = UUID.randomUUID();

    @Test
    void escenario1UnassigningDeactivatesTheAssignmentInsteadOfDeletingIt() {
        var assignment = new VeterinaryAssignment(vet, client);
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(assignment));

        service.handle(new UnassignClientCommand(id));

        assertEquals(VeterinaryAssignmentStatus.INACTIVE, assignment.getStatus());
        verify(repository).save(assignment);
        verify(repository, never()).delete(any(VeterinaryAssignment.class));
        verify(repository, never()).deleteById(any(UUID.class));
    }

    @Test
    void escenario2AnUnknownAssignmentIsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AssignmentNotFoundException.class, () -> service.handle(new UnassignClientCommand(id)));

        verify(repository, never()).save(any(VeterinaryAssignment.class));
    }

    @Test
    void unassigningTwiceTheSecondTimeIsNotFoundBecauseItWasAlreadyUnassigned() {
        var assignment = new VeterinaryAssignment(vet, client);
        assignment.deactivate();
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(assignment));

        assertThrows(AssignmentNotFoundException.class, () -> service.handle(new UnassignClientCommand(id)));
    }

    @Test
    void assigningAnAlreadyAssignedClientIsAConflict() {
        when(repository.existsByVeterinarianIdAndClientIdAndStatus(vet, client, VeterinaryAssignmentStatus.ACTIVE))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.handle(new AssignClientCommand(vet, client)));
    }

    @Test
    void aClientUnassignedBeforeCanBeAssignedAgain() {
        when(repository.existsByVeterinarianIdAndClientIdAndStatus(vet, client, VeterinaryAssignmentStatus.ACTIVE))
                .thenReturn(false);
        when(repository.save(any(VeterinaryAssignment.class))).thenAnswer(call -> call.getArgument(0));

        var assignment = service.handle(new AssignClientCommand(vet, client));

        assertEquals(VeterinaryAssignmentStatus.ACTIVE, assignment.getStatus());
    }
}
