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
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareConflictException;
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
    private final CareRecordCommandServiceImpl service = new CareRecordCommandServiceImpl(repository, writer);

    @BeforeEach
    void setUp() {
        when(repository.saveAndFlush(any(CareRecord.class))).thenAnswer(call -> call.getArgument(0));
        when(repository.save(any(CareRecord.class))).thenAnswer(call -> call.getArgument(0));
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
}
