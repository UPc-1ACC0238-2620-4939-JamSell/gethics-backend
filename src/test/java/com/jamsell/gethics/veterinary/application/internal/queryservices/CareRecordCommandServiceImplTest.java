package com.jamsell.gethics.veterinary.application.internal.commandservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalHistoryWriter;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.PatientAccessLookup;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientAccess;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareConflictException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.ClinicalHistorySyncException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientAccessDeniedException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientNotActiveException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientNotFoundException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientWithoutClientException;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncStatus;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.CareRecordRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CareRecordCommandServiceImplTest {

    private final CareRecordRepository repository = mock(CareRecordRepository.class);
    private final ClinicalHistoryWriter writer = mock(ClinicalHistoryWriter.class);
    private final VeterinaryAssignmentRepository assignments = mock(VeterinaryAssignmentRepository.class);
    private final PatientAccessLookup patients = mock(PatientAccessLookup.class);
    private final CareRecordCommandServiceImpl service =
            new CareRecordCommandServiceImpl(repository, writer, assignments, patients);

    @BeforeEach
    void setUp() {
        when(repository.saveAndFlush(any(CareRecord.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.save(any(CareRecord.class))).thenAnswer(call -> call.getArgument(0));
        when(patients.findByPatientId(any(UUID.class)))
                .thenAnswer(call -> Optional.of(new PatientAccess(call.getArgument(0), true, UUID.randomUUID())));
        when(assignments.existsByVeterinarianIdAndClientIdAndStatus(any(UUID.class), any(UUID.class),
                any(VeterinaryAssignmentStatus.class))).thenReturn(true);
    }

    private static RegisterCareCommand command(UUID requestId, String diagnosis) {
        return new RegisterCareCommand(requestId, UUID.randomUUID(), UUID.randomUUID(),
                diagnosis, "Antibiotic", null, null);
    }

    @Test
    void newRequestIsSavedAndPushedToHistory() {
        var command = command(UUID.randomUUID(), "Mastitis");
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.empty());

        var result = service.handle(command);

        assertTrue(result.created());
        assertTrue(result.record().isHistorySynced());
        verify(writer).append(any(CareRecord.class));
    }

    @Test
    void repeatedRequestReturnsExistingWithoutPushingAgain() {
        var command = command(UUID.randomUUID(), "Mastitis");
        var existing = new CareRecord(command);
        existing.markHistorySynced();
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.of(existing));

        var result = service.handle(command);

        assertFalse(result.created());
        verify(writer, never()).append(any(CareRecord.class));
    }

    @Test
    void sameRequestIdWithDifferentContentThrowsConflict() {
        var original = command(UUID.randomUUID(), "Mastitis");
        var existing = new CareRecord(original);
        var changed = new RegisterCareCommand(original.clientRequestId(), original.veterinarianId(),
                original.patientId(), "Other", original.treatment(), null, null);
        when(repository.findByClientRequestId(original.clientRequestId())).thenReturn(Optional.of(existing));

        assertThrows(CareConflictException.class, () -> service.handle(changed));
    }

    @Test
    void batchReportsEachItemIndependently() {
        var valid = command(UUID.randomUUID(), "Mastitis");
        var invalid = command(UUID.randomUUID(), " ");
        when(repository.findByClientRequestId(any(UUID.class))).thenReturn(Optional.empty());

        var results = service.handle(new SyncCareBatchCommand(List.of(valid, invalid)));

        assertEquals(CareSyncStatus.CREATED, results.get(0).status());
        assertEquals(CareSyncStatus.REJECTED, results.get(1).status());
    }

    // ---- TS07: el paciente debe existir, estar activo y ser de un cliente asignado al veterinario ----

    private void givenPatient(RegisterCareCommand command, boolean active, UUID clientId, boolean assigned) {
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.empty());
        when(patients.findByPatientId(command.patientId()))
                .thenReturn(Optional.of(new PatientAccess(command.patientId(), active, clientId)));
        when(assignments.existsByVeterinarianIdAndClientIdAndStatus(command.veterinarianId(), clientId,
                VeterinaryAssignmentStatus.ACTIVE)).thenReturn(assigned);
    }

    @Test
    void careOnAnUnknownPatientIsRejectedAndNothingIsSaved() {
        var command = command(UUID.randomUUID(), "Mastitis");
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.empty());
        when(patients.findByPatientId(command.patientId())).thenReturn(Optional.empty());

        assertThrows(PatientNotFoundException.class, () -> service.handle(command));

        verify(repository, never()).saveAndFlush(any(CareRecord.class));
        verify(writer, never()).append(any(CareRecord.class));
    }

    @Test
    void careOnAnInactivePatientIsRejected() {
        var command = command(UUID.randomUUID(), "Mastitis");
        givenPatient(command, false, UUID.randomUUID(), true);

        assertThrows(PatientNotActiveException.class, () -> service.handle(command));

        verify(repository, never()).saveAndFlush(any(CareRecord.class));
    }

    @Test
    void careOnAPatientWithoutFarmIsRejectedBecauseItsClientIsUnknown() {
        var command = command(UUID.randomUUID(), "Mastitis");
        givenPatient(command, true, null, true);

        assertThrows(PatientWithoutClientException.class, () -> service.handle(command));
    }

    @Test
    void careByAVeterinarianNotAssignedToTheClientIsDenied() {
        var command = command(UUID.randomUUID(), "Mastitis");
        givenPatient(command, true, UUID.randomUUID(), false);

        assertThrows(PatientAccessDeniedException.class, () -> service.handle(command));

        verify(repository, never()).saveAndFlush(any(CareRecord.class));
    }

    @Test
    void aRetryOfAnExistingCareDoesNotRevalidateThePatient() {
        var command = command(UUID.randomUUID(), "Mastitis");
        var existing = new CareRecord(command);
        existing.markHistorySynced();
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.of(existing));

        service.handle(command);

        verify(patients, never()).findByPatientId(any(UUID.class));
    }

    @Test
    void ifTheHistoryCannotBeUpdatedTheCareStaysSavedAndTheFailureIsReportedAsRetryable() {
        var command = command(UUID.randomUUID(), "Mastitis");
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new IllegalStateException("sanitary down")).when(writer).append(any(CareRecord.class));

        assertThrows(ClinicalHistorySyncException.class, () -> service.handle(command));

        verify(repository).saveAndFlush(any(CareRecord.class));
        verify(repository, never()).save(any(CareRecord.class));
    }

    @Test
    void aRetryAfterAHistoryFailureCompletesTheSyncWithoutDuplicatingTheCare() {
        var command = command(UUID.randomUUID(), "Mastitis");
        var pending = new CareRecord(command);
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.of(pending));

        var result = service.handle(command);

        assertFalse(result.created());
        assertTrue(pending.isHistorySynced());
        verify(writer).append(pending);
        verify(repository, never()).saveAndFlush(any(CareRecord.class));
    }

    @Test
    void batchReportsRejectedForBusinessRulesAndFailedForRetryableHistoryErrors() {
        var unknownPatient = command(UUID.randomUUID(), "Mastitis");
        var historyDown = command(UUID.randomUUID(), "Cojera");
        var fine = command(UUID.randomUUID(), "Fiebre");
        when(repository.findByClientRequestId(any(UUID.class))).thenReturn(Optional.empty());
        when(patients.findByPatientId(unknownPatient.patientId())).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new IllegalStateException("down")).when(writer)
                .append(org.mockito.ArgumentMatchers.argThat(r -> r != null && "Cojera".equals(r.getDiagnosis())));

        var results = service.handle(new SyncCareBatchCommand(List.of(unknownPatient, historyDown, fine)));

        assertEquals(CareSyncStatus.REJECTED, results.get(0).status());
        assertEquals(CareSyncStatus.FAILED, results.get(1).status());
        assertEquals(CareSyncStatus.CREATED, results.get(2).status());
    }

    @Test
    void aConcurrentInsertOfTheSameRequestResolvesToTheWinnerInsteadOfFailing() {
        var command = command(UUID.randomUUID(), "Mastitis");
        var winner = new CareRecord(command);
        winner.markHistorySynced();
        when(repository.findByClientRequestId(command.clientRequestId()))
                .thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.saveAndFlush(any(CareRecord.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("uk_client_request_id"));

        var result = service.handle(command);

        assertFalse(result.created());
        assertEquals(winner, result.record());
    }

    @Test
    void anIntegrityErrorThatIsNotAConcurrentDuplicateIsNotSwallowed() {
        var command = command(UUID.randomUUID(), "Mastitis");
        when(repository.findByClientRequestId(command.clientRequestId())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CareRecord.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("other"));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.handle(command));
    }

    @Test
    void batchReportsTheRetriedItemAsDuplicateAndAChangedPayloadAsConflict() {
        var original = command(UUID.randomUUID(), "Mastitis");
        var existing = new CareRecord(original);
        existing.markHistorySynced();
        var changed = new RegisterCareCommand(original.clientRequestId(), original.veterinarianId(),
                original.patientId(), "Other", original.treatment(), null, null);
        when(repository.findByClientRequestId(original.clientRequestId())).thenReturn(Optional.of(existing));

        var results = service.handle(new SyncCareBatchCommand(List.of(original, changed)));

        assertEquals(CareSyncStatus.DUPLICATE, results.get(0).status());
        assertEquals(CareSyncStatus.CONFLICT, results.get(1).status());
        assertEquals(existing.getId(), results.get(0).careRecordId());
    }
}
