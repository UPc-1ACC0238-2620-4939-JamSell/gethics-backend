package com.jamsell.gethics.sanitary.application.internal.eventhandlers;

import com.jamsell.gethics.livestock.domain.model.events.AnimalLeftActiveHerdEvent;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CancelAllScheduledEventsCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnimalLeftActiveHerdEventHandlerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void theHandlerCancelsTheScheduledEventsOfTheAnimalThatLeftTheHerd() {
        var commands = mock(ClinicalHistoryCommandService.class);
        var animalId = UUID.randomUUID();

        new AnimalLeftActiveHerdEventHandler(commands).on(new AnimalLeftActiveHerdEvent(animalId, AnimalStatus.INACTIVE));

        verify(commands).handle(new CancelAllScheduledEventsCommand(animalId));
    }

    @Test
    void onlyScheduledEventsAreCancelledAndTheClinicalHistoryIsKept() {
        var history = new ClinicalHistory(UUID.randomUUID());
        var done = history.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2025, 5, 1, 9, 0), "Aftosa");
        var pending1 = history.scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2027, 1, 1), null, TODAY);
        var pending2 = history.scheduleEvent(SanitaryEventType.CHECKUP, LocalDate.of(2027, 2, 1), null, TODAY);
        var alreadyCancelled = history.scheduleEvent(SanitaryEventType.OTHER, LocalDate.of(2027, 3, 1), null, TODAY);
        alreadyCancelled.cancel();

        var cancelled = history.cancelAllScheduledEvents();

        assertEquals(2, cancelled);
        assertEquals(SanitaryEventStatus.COMPLETED, done.getStatus());
        assertEquals(SanitaryEventStatus.CANCELLED, pending1.getStatus());
        assertEquals(SanitaryEventStatus.CANCELLED, pending2.getStatus());
        assertEquals(4, history.getEvents().size());
        assertEquals(0, history.cancelAllScheduledEvents());
    }
}
