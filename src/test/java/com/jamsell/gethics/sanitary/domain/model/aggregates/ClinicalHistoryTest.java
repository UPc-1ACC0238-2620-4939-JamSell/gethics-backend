package com.jamsell.gethics.sanitary.domain.model.aggregates;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.PastScheduledDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotScheduledException;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalHistoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 3);

    private final ClinicalHistory history = new ClinicalHistory(UUID.randomUUID());

    @Test
    void registersPastEventAsCompletedInsideTheHistory() {
        var occurredAt = LocalDate.now().minusDays(3).atTime(10, 30);

        var event = history.registerEvent(SanitaryEventType.VACCINATION, occurredAt, "Aftosa");

        assertEquals(1, history.getEvents().size());
        assertSame(event, history.getEvents().get(0));
        assertSame(history, event.getClinicalHistory());
        assertEquals(SanitaryEventType.VACCINATION, event.getType());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals("Aftosa", event.getDescription());
        assertEquals(SanitaryEventStatus.COMPLETED, event.getStatus());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void acceptsAnyTimeTodayAndNullDescription() {
        var endOfToday = LocalDate.now().atTime(23, 59, 59);

        var event = history.registerEvent(SanitaryEventType.CHECKUP, endOfToday, null);

        assertEquals(endOfToday, event.getOccurredAt());
        assertNull(event.getDescription());
    }

    @Test
    void rejectsDateAfterTodayAndDoesNotAddTheEvent() {
        var tomorrow = LocalDate.now().plusDays(1).atStartOfDay();

        assertThrows(FutureEventDateException.class,
                () -> history.registerEvent(SanitaryEventType.TREATMENT, tomorrow, "x"));
        assertTrue(history.getEvents().isEmpty());
    }

    @Test
    void registeredEventHasNoScheduledDate() {
        var event = history.registerEvent(SanitaryEventType.CHECKUP, LocalDateTime.now().minusDays(1), null);

        assertNull(event.getScheduledDate());
    }

    @Test
    void schedulesEventWithScheduledDateAndNoOccurredAt() {
        var date = LocalDate.of(2026, 10, 15);

        var event = history.scheduleEvent(SanitaryEventType.VACCINATION, date, "Brucelosis", TODAY);

        assertEquals(1, history.getEvents().size());
        assertSame(event, history.getEvents().get(0));
        assertSame(history, event.getClinicalHistory());
        assertEquals(SanitaryEventStatus.SCHEDULED, event.getStatus());
        assertEquals(date, event.getScheduledDate());
        assertNull(event.getOccurredAt());
        assertEquals("Brucelosis", event.getDescription());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void rejectsNullScheduledDateAndDoesNotAddTheEvent() {
        assertThrows(NullPointerException.class,
                () -> history.scheduleEvent(SanitaryEventType.CHECKUP, null, null, TODAY));
        assertTrue(history.getEvents().isEmpty());
    }

    @Test
    void schedulingAnEventTouchesUpdatedAt() throws InterruptedException {
        var before = history.getUpdatedAt();
        Thread.sleep(2);

        history.scheduleEvent(SanitaryEventType.OTHER, TODAY, null, TODAY);

        assertTrue(history.getUpdatedAt().isAfter(before));
    }

    @Test
    void registeringAnEventTouchesUpdatedAt() throws InterruptedException {
        var before = history.getUpdatedAt();
        Thread.sleep(2);

        history.registerEvent(SanitaryEventType.OTHER, LocalDateTime.now().minusMinutes(1), null);

        assertTrue(history.getUpdatedAt().isAfter(before));
    }

    @Test
    void schedulesForTodayButRejectsAPastDateAndDoesNotAddTheEvent() {
        assertEquals(TODAY, history.scheduleEvent(SanitaryEventType.CHECKUP, TODAY, null, TODAY).getScheduledDate());

        assertThrows(PastScheduledDateException.class,
                () -> history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.minusDays(1), null, TODAY));
        assertEquals(1, history.getEvents().size());
    }

    @Test
    void rejectsSchedulingWithoutAReferenceDate() {
        assertThrows(NullPointerException.class,
                () -> history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY, null, null));
        assertTrue(history.getEvents().isEmpty());
    }

    private SanitaryEvent scheduledVaccinationWithId() {
        var event = history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(3), "Aftosa", TODAY);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    @Test
    void completingAScheduledEventTransitionsTheSameEventAndKeepsItsScheduledDate() throws InterruptedException {
        var scheduled = scheduledVaccinationWithId();
        var before = history.getUpdatedAt();
        Thread.sleep(2);
        var occurredAt = TODAY.atTime(9, 15);

        var completed = history.completeScheduledEvent(scheduled.getId(), occurredAt, null, TODAY);

        assertSame(scheduled, completed);
        assertEquals(1, history.getEvents().size());
        assertEquals(SanitaryEventStatus.COMPLETED, completed.getStatus());
        assertEquals(occurredAt, completed.getOccurredAt());
        assertEquals(TODAY.plusDays(3), completed.getScheduledDate());
        assertEquals("Aftosa", completed.getDescription());
        assertTrue(history.getUpdatedAt().isAfter(before));
    }

    @Test
    void completingReplacesTheDescriptionOnlyWhenOneIsGiven() {
        var scheduled = scheduledVaccinationWithId();

        history.completeScheduledEvent(scheduled.getId(), TODAY.atTime(9, 0), "Aftosa lote 42", TODAY);

        assertEquals("Aftosa lote 42", scheduled.getDescription());
    }

    @Test
    void anEventCannotBeCompletedTwice() {
        var scheduled = scheduledVaccinationWithId();
        history.completeScheduledEvent(scheduled.getId(), TODAY.atTime(9, 0), null, TODAY);

        assertThrows(SanitaryEventNotScheduledException.class,
                () -> history.completeScheduledEvent(scheduled.getId(), TODAY.atTime(10, 0), null, TODAY));
        assertEquals(TODAY.atTime(9, 0), scheduled.getOccurredAt());
    }

    @Test
    void completingWithAFutureDateIsRejectedAndTheEventStaysScheduled() {
        var scheduled = scheduledVaccinationWithId();

        assertThrows(FutureEventDateException.class,
                () -> history.completeScheduledEvent(scheduled.getId(), TODAY.plusDays(1).atStartOfDay(), null, TODAY));
        assertEquals(SanitaryEventStatus.SCHEDULED, scheduled.getStatus());
        assertNull(scheduled.getOccurredAt());
    }

    @Test
    void completingAnEventOfAnotherHistoryIsNotFound() {
        scheduledVaccinationWithId();

        assertThrows(SanitaryEventNotFoundException.class,
                () -> history.completeScheduledEvent(UUID.randomUUID(), TODAY.atTime(9, 0), null, TODAY));
    }
}
