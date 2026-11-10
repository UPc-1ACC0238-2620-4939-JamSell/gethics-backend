package com.jamsell.gethics.sanitary.domain.model.aggregates;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.PastScheduledDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotScheduledException;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
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

    // ---- US-33: cancelacion ----

    @Test
    void cancellingAScheduledEventKeepsTheSameEventInTheHistoryAsCancelled() throws InterruptedException {
        var scheduled = scheduledVaccinationWithId();
        var before = history.getUpdatedAt();
        Thread.sleep(2);

        var cancelled = history.cancelScheduledEvent(scheduled.getId());

        assertSame(scheduled, cancelled);
        assertEquals(1, history.getEvents().size()); // no se elimina
        assertEquals(SanitaryEventStatus.CANCELLED, cancelled.getStatus());
        assertEquals(TODAY.plusDays(3), cancelled.getScheduledDate());
        assertEquals("Aftosa", cancelled.getDescription());
        assertNull(cancelled.getOccurredAt());
        assertTrue(history.getUpdatedAt().isAfter(before));
    }

    @Test
    void aCompletedEventCannotBeCancelled() {
        var scheduled = scheduledVaccinationWithId();
        history.completeScheduledEvent(scheduled.getId(), TODAY.atTime(9, 0), null, TODAY);

        var ex = assertThrows(SanitaryEventNotScheduledException.class, () -> history.cancelScheduledEvent(scheduled.getId()));

        assertEquals("Solo un evento programado (SCHEDULED) puede cancelarse.", ex.getMessage());
        assertEquals(SanitaryEventStatus.COMPLETED, scheduled.getStatus());
    }

    @Test
    void anEventRegisteredAsCompletedByUs11CannotBeCancelled() {
        var completed = history.registerEvent(SanitaryEventType.CHECKUP, LocalDate.now().minusDays(1).atTime(8, 0), null);

        assertThrows(SanitaryEventNotScheduledException.class, completed::cancel);
        assertEquals(SanitaryEventStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void anEventCannotBeCancelledTwice() {
        var scheduled = scheduledVaccinationWithId();
        history.cancelScheduledEvent(scheduled.getId());

        assertThrows(SanitaryEventNotScheduledException.class, () -> history.cancelScheduledEvent(scheduled.getId()));
        assertEquals(SanitaryEventStatus.CANCELLED, scheduled.getStatus());
    }

    @Test
    void aCancelledEventCannotBeCompleted() {
        var scheduled = scheduledVaccinationWithId();
        history.cancelScheduledEvent(scheduled.getId());

        assertThrows(SanitaryEventNotScheduledException.class,
                () -> history.completeScheduledEvent(scheduled.getId(), TODAY.atTime(9, 0), null, TODAY));
        assertEquals(SanitaryEventStatus.CANCELLED, scheduled.getStatus());
    }

    @Test
    void cancellingAnEventOfAnotherHistoryIsNotFound() {
        var scheduled = scheduledVaccinationWithId();

        assertThrows(SanitaryEventNotFoundException.class, () -> history.cancelScheduledEvent(UUID.randomUUID()));
        assertEquals(SanitaryEventStatus.SCHEDULED, scheduled.getStatus());
    }

    // ---- US-35: clientRequestId y comparacion del request ----

    private static final UUID REQUEST_ID = UUID.randomUUID();

    @Test
    void eventsKeepTheClientRequestIdAndItIsOptional() {
        var registered = history.registerEvent(SanitaryEventType.CHECKUP, TODAY.atTime(8, 0), null, REQUEST_ID);
        var scheduled = history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(1), null, TODAY, UUID.randomUUID());
        var legacy = history.registerEvent(SanitaryEventType.CHECKUP, TODAY.atTime(8, 0), null);

        assertEquals(REQUEST_ID, registered.getClientRequestId());
        assertNotNull(scheduled.getClientRequestId());
        assertNull(legacy.getClientRequestId());
    }

    @Test
    void aRegisteredEventMatchesOnlyTheSameRegisterRequest() {
        var occurredAt = TODAY.atTime(8, 0);
        var event = history.registerEvent(SanitaryEventType.TREATMENT, occurredAt, "Ivermectina", REQUEST_ID);
        var animalId = history.getAnimalId();

        assertTrue(event.matches(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt, "Ivermectina", REQUEST_ID)));
        assertFalse(event.matches(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.DISEASE, occurredAt, "Ivermectina", REQUEST_ID)));
        assertFalse(event.matches(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt.plusMinutes(1), "Ivermectina", REQUEST_ID)));
        assertFalse(event.matches(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt, "otra", REQUEST_ID)));
        assertFalse(event.matches(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt, null, REQUEST_ID)));
        assertFalse(event.matches(new RegisterSanitaryEventCommand(UUID.randomUUID(), SanitaryEventType.TREATMENT, occurredAt, "Ivermectina", REQUEST_ID)));
        // Mismo id reutilizado para la otra operacion
        assertFalse(event.matches(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, TODAY, "Ivermectina", REQUEST_ID)));
    }

    @Test
    void aScheduledEventMatchesOnlyTheSameScheduleRequestWhateverItsStatus() {
        var date = TODAY.plusDays(3);
        var event = history.scheduleEvent(SanitaryEventType.VACCINATION, date, "Aftosa", TODAY, REQUEST_ID);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        var animalId = history.getAnimalId();
        var same = new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, date, "Aftosa", REQUEST_ID);

        assertTrue(event.matches(same));
        assertFalse(event.matches(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, date.plusDays(1), "Aftosa", REQUEST_ID)));
        assertFalse(event.matches(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.CHECKUP, date, "Aftosa", REQUEST_ID)));
        assertFalse(event.matches(new ScheduleSanitaryEventCommand(UUID.randomUUID(), SanitaryEventType.VACCINATION, date, "Aftosa", REQUEST_ID)));
        assertFalse(event.matches(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, TODAY.atTime(8, 0), "Aftosa", REQUEST_ID)));

        history.cancelScheduledEvent(event.getId()); // un reintento tardio sigue siendo el mismo request
        assertTrue(event.matches(same));
    }
}
