package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SanitaryScheduleServiceTest {

    // Fecha en que se programaron los fixtures: el dominio no permite programar en una fecha ya vencida.
    private static final LocalDate SCHEDULED_ON = LocalDate.of(2026, 1, 1);

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private final SanitaryScheduleService service = new SanitaryScheduleService();
    private final ClinicalHistory history = new ClinicalHistory(UUID.randomUUID());

    private SanitaryEvent scheduled(SanitaryEventType type, LocalDate date) {
        return history.scheduleEvent(type, date, null, SCHEDULED_ON);
    }

    @Test
    void remindsScheduledVaccinationExactlyThreeDaysAhead() {
        assertTrue(service.needsReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(3)), TODAY));
    }

    @Test
    void doesNotRemindTwoOrFourDaysAhead() {
        assertFalse(service.needsReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(2)), TODAY));
        assertFalse(service.needsReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(4)), TODAY));
    }

    @Test
    void doesNotRemindOtherEventTypes() {
        assertFalse(service.needsReminder(scheduled(SanitaryEventType.TREATMENT, TODAY.plusDays(3)), TODAY));
        assertFalse(service.needsReminder(scheduled(SanitaryEventType.CHECKUP, TODAY.plusDays(3)), TODAY));
    }

    @Test
    void doesNotRemindCompletedEvents() {
        var completed = history.registerEvent(SanitaryEventType.VACCINATION, TODAY.minusDays(1).atTime(9, 0), "aplicada");
        assertFalse(service.needsReminder(completed, TODAY));

        // El MISMO evento programado ya completado (no existe aun la transicion: se simula el estado).
        var scheduledThenCompleted = scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(3));
        ReflectionTestUtils.setField(scheduledThenCompleted, "status", SanitaryEventStatus.COMPLETED);
        assertFalse(service.needsReminder(scheduledThenCompleted, TODAY));
    }

    @Test
    void doesNotRemindCancelledEvents() {
        var cancelled = scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(3));
        ReflectionTestUtils.setField(cancelled, "status", SanitaryEventStatus.CANCELLED);
        assertFalse(service.needsReminder(cancelled, TODAY));
    }

    @Test
    void generatesPendingReminderAtStartOfTheReminderDay() {
        var event = scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(3));

        var reminder = service.generateReminder(event);

        assertSame(event, reminder.getSanitaryEvent());
        assertEquals(TODAY.atStartOfDay(), reminder.getScheduledFor());
        assertEquals(LocalDateTime.of(2026, 10, 2, 0, 0), service.reminderTimeFor(event.getScheduledDate()));
        assertEquals(ReminderStatus.PENDING, reminder.getStatus());
        assertEquals(0, reminder.getAttempts());
    }

    @Test
    void failedReminderIsRetryableWhileTheEventIsScheduledAndNotPast() {
        var upcoming = service.generateReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(2)));
        var dueToday = service.generateReminder(scheduled(SanitaryEventType.VACCINATION, TODAY));
        upcoming.markFailed();
        dueToday.markFailed();

        assertTrue(service.canRetry(upcoming, TODAY));
        assertTrue(service.canRetry(dueToday, TODAY));
    }

    @Test
    void failedReminderIsNotRetryableOncePastOrNoLongerScheduled() {
        var past = service.generateReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.minusDays(1)));
        past.markFailed();
        assertFalse(service.canRetry(past, TODAY));

        var completed = scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(2));
        var reminder = service.generateReminder(completed);
        reminder.markFailed();
        ReflectionTestUtils.setField(completed, "status", SanitaryEventStatus.COMPLETED);
        assertFalse(service.canRetry(reminder, TODAY));
    }

    @Test
    void onlyFailedRemindersAreRetryable() {
        var pending = service.generateReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(2)));
        assertFalse(service.canRetry(pending, TODAY));

        var sent = service.generateReminder(scheduled(SanitaryEventType.VACCINATION, TODAY.plusDays(2)));
        sent.markSent(java.time.Instant.now());
        assertFalse(service.canRetry(sent, TODAY));
    }
}
