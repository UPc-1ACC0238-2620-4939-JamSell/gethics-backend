package com.jamsell.gethics.sanitary.domain.model.aggregates;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalHistoryTest {

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
    void registeringAnEventTouchesUpdatedAt() throws InterruptedException {
        var before = history.getUpdatedAt();
        Thread.sleep(2);

        history.registerEvent(SanitaryEventType.OTHER, LocalDateTime.now().minusMinutes(1), null);

        assertTrue(history.getUpdatedAt().isAfter(before));
    }
}
