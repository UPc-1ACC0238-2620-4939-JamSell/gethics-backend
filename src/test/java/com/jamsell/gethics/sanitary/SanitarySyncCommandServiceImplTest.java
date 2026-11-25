package com.jamsell.gethics.sanitary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jamsell.gethics.sanitary.application.internal.commandservices.SanitarySyncCommandServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.SyncOfflineSanitaryEventItem;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventCreation;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SanitarySyncCommandServiceImplTest {

    @Mock
    private ClinicalHistoryCommandService clinicalHistoryCommandService;

    @InjectMocks
    private SanitarySyncCommandServiceImpl syncCommandService;

    @Test
    void shouldSyncNewEventAndSkipAlreadySyncedEvent() {
        var animalId = UUID.randomUUID();
        var clientId1 = UUID.randomUUID().toString();
        var clientId2 = UUID.randomUUID().toString();
        var now = LocalDateTime.of(2026, 11, 24, 10, 0);
        var eventTypeName = SanitaryEventType.values()[0].name();

        var newItem = new SyncOfflineSanitaryEventItem(clientId1, animalId, eventTypeName, "Aftosa", now);
        var dupItem = new SyncOfflineSanitaryEventItem(clientId2, animalId, eventTypeName, "Control", now);

        var createdEvent = mock(SanitaryEvent.class);
        var existingEvent = mock(SanitaryEvent.class);

        when(clinicalHistoryCommandService.handle(any(RegisterSanitaryEventCommand.class)))
                .thenReturn(new SanitaryEventCreation(createdEvent, true))
                .thenReturn(new SanitaryEventCreation(existingEvent, false));

        var response = syncCommandService.syncBatch(List.of(newItem, dupItem));

        assertEquals(2, response.totalReceived());
        assertEquals(2, response.syncedCount());
        assertEquals(0, response.failedCount());
        assertEquals("SYNCED", response.results().get(0).status());
        assertEquals("ALREADY_SYNCED", response.results().get(1).status());
    }
}
