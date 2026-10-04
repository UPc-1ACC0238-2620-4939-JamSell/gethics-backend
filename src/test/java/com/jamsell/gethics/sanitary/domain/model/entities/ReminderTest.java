package com.jamsell.gethics.sanitary.domain.model.entities;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReminderTest {

    // Fecha en que se programaron los fixtures: el dominio no permite programar en una fecha ya vencida.
    private static final LocalDate SCHEDULED_ON = LocalDate.of(2026, 10, 2);

    private final SanitaryEvent event = new ClinicalHistory(UUID.randomUUID())
            .scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2026, 10, 5), null, SCHEDULED_ON);
    private final LocalDateTime reminderTime = LocalDate.of(2026, 10, 2).atStartOfDay();

    @Test
    void startsPendingWithoutAttempts() {
        var reminder = Reminder.create(event, reminderTime);

        assertEquals(ReminderStatus.PENDING, reminder.getStatus());
        assertEquals(0, reminder.getAttempts());
        assertNull(reminder.getSentAt());
        assertNotNull(reminder.getCreatedAt());
        assertSame(event, reminder.getSanitaryEvent());
    }

    @Test
    void markSentRecordsTimeAndAttempt() {
        var reminder = Reminder.create(event, reminderTime);
        var now = Instant.parse("2026-10-02T13:00:00Z");

        reminder.markSent(now);

        assertEquals(ReminderStatus.SENT, reminder.getStatus());
        assertEquals(now, reminder.getSentAt());
        assertEquals(1, reminder.getAttempts());
    }

    @Test
    void failedThenSentCountsEveryAttempt() {
        var reminder = Reminder.create(event, reminderTime);

        reminder.markFailed();
        assertEquals(ReminderStatus.FAILED, reminder.getStatus());
        assertEquals(1, reminder.getAttempts());

        reminder.markFailed();
        assertEquals(2, reminder.getAttempts());

        reminder.markSent(Instant.now());
        assertEquals(ReminderStatus.SENT, reminder.getStatus());
        assertEquals(3, reminder.getAttempts());
    }

    @Test
    void sentReminderCannotChangeAgain() {
        var reminder = Reminder.create(event, reminderTime);
        reminder.markSent(Instant.now());

        assertThrows(IllegalStateException.class, reminder::markFailed);
        assertThrows(IllegalStateException.class, () -> reminder.markSent(Instant.now()));
    }

    @Test
    void sameEventMayHaveRemindersForDifferentTimes() {
        var threeDays = Reminder.create(event, reminderTime);
        var oneDay = Reminder.create(event, reminderTime.plusDays(2));

        assertNotEquals(threeDays.getScheduledFor(), oneDay.getScheduledFor());
        assertSame(threeDays.getSanitaryEvent(), oneDay.getSanitaryEvent());
    }
}
