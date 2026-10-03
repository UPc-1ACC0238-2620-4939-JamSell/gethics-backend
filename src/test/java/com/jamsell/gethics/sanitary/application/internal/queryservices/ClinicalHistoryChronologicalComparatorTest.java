package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalHistoryChronologicalComparatorTest {

    private final ClinicalHistory history = new ClinicalHistory(UUID.randomUUID());

    private SanitaryEvent completed(LocalDateTime occurredAt) {
        return history.registerEvent(SanitaryEventType.VACCINATION, occurredAt, "completed " + occurredAt);
    }

    private SanitaryEvent scheduled(LocalDate date) {
        return history.scheduleEvent(SanitaryEventType.CHECKUP, date, "scheduled " + date);
    }

    private SanitaryEvent withStatus(SanitaryEvent event, SanitaryEventStatus status) {
        ReflectionTestUtils.setField(event, "status", status);
        return event;
    }

    private SanitaryEvent withCreatedAt(SanitaryEvent event, Instant createdAt) {
        ReflectionTestUtils.setField(event, "createdAt", createdAt);
        return event;
    }

    private SanitaryEvent withId(SanitaryEvent event, String uuid) {
        ReflectionTestUtils.setField(event, "id", UUID.fromString(uuid));
        return event;
    }

    private List<SanitaryEvent> sorted(SanitaryEvent... events) {
        var list = new ArrayList<>(List.of(events));
        list.sort(ClinicalHistoryChronologicalComparator.INSTANCE);
        return list;
    }

    @Test
    void completedEventUsesOccurredAt() {
        var event = completed(LocalDateTime.of(2026, 3, 1, 10, 30));

        assertEquals(LocalDateTime.of(2026, 3, 1, 10, 30), ClinicalHistoryChronologicalComparator.effectiveDateTime(event));
    }

    @Test
    void scheduledEventUsesScheduledDateAtMidnight() {
        var event = scheduled(LocalDate.of(2026, 11, 5));

        assertEquals(LocalDateTime.of(2026, 11, 5, 0, 0), ClinicalHistoryChronologicalComparator.effectiveDateTime(event));
    }

    @Test
    void cancelledEventUsesItsScheduledDate() {
        var event = withStatus(scheduled(LocalDate.of(2026, 4, 15)), SanitaryEventStatus.CANCELLED);

        assertEquals(LocalDateTime.of(2026, 4, 15, 0, 0), ClinicalHistoryChronologicalComparator.effectiveDateTime(event));
    }

    @Test
    void mixedEventsAreOrderedAscendingWithFutureOnesLast() {
        var old = completed(LocalDateTime.of(2025, 12, 1, 8, 0));
        var overdue = scheduled(LocalDate.of(2026, 2, 10));
        var recent = completed(LocalDateTime.of(2026, 3, 1, 10, 0));
        var cancelled = withStatus(scheduled(LocalDate.of(2026, 4, 15)), SanitaryEventStatus.CANCELLED);
        var future = scheduled(LocalDate.of(2027, 1, 20));

        assertEquals(List.of(old, overdue, recent, cancelled, future), sorted(future, recent, old, cancelled, overdue));
    }

    @Test
    void scheduledEventOfTheSameDayComesBeforeACompletedOne() {
        var completed = completed(LocalDateTime.of(2026, 3, 1, 10, 0));
        var scheduled = scheduled(LocalDate.of(2026, 3, 1));

        assertEquals(List.of(scheduled, completed), sorted(completed, scheduled));
    }

    @Test
    void sameEffectiveDateIsBrokenByCreatedAtThenById() {
        var date = LocalDate.of(2026, 5, 1);
        var first = withCreatedAt(withId(scheduled(date), "00000000-0000-0000-0000-000000000009"), Instant.parse("2026-01-01T00:00:00Z"));
        var second = withCreatedAt(withId(scheduled(date), "00000000-0000-0000-0000-000000000001"), Instant.parse("2026-01-02T00:00:00Z"));
        var sameCreatedLowId = withCreatedAt(withId(scheduled(date), "00000000-0000-0000-0000-000000000002"), Instant.parse("2026-01-03T00:00:00Z"));
        var sameCreatedHighId = withCreatedAt(withId(scheduled(date), "00000000-0000-0000-0000-000000000003"), Instant.parse("2026-01-03T00:00:00Z"));

        var expected = List.of(first, second, sameCreatedLowId, sameCreatedHighId);

        assertEquals(expected, sorted(sameCreatedHighId, second, first, sameCreatedLowId));
        assertEquals(expected, sorted(first, sameCreatedLowId, sameCreatedHighId, second));
    }

    @Test
    void nullDatesFallBackToCreatedAtWithoutFailing() {
        var createdAt = Instant.parse("2026-06-01T15:00:00Z");
        var completedWithoutOccurredAt = withCreatedAt(completed(LocalDateTime.of(2020, 1, 1, 0, 0)), createdAt);
        ReflectionTestUtils.setField(completedWithoutOccurredAt, "occurredAt", null);
        var scheduledWithoutDate = withCreatedAt(scheduled(LocalDate.of(2020, 1, 1)), createdAt);
        ReflectionTestUtils.setField(scheduledWithoutDate, "scheduledDate", null);
        var cancelledWithoutDate = withCreatedAt(withStatus(scheduled(LocalDate.of(2020, 1, 1)), SanitaryEventStatus.CANCELLED), createdAt);
        ReflectionTestUtils.setField(cancelledWithoutDate, "scheduledDate", null);

        var expected = LocalDateTime.ofInstant(createdAt, ZoneId.systemDefault());
        assertEquals(expected, ClinicalHistoryChronologicalComparator.effectiveDateTime(completedWithoutOccurredAt));
        assertEquals(expected, ClinicalHistoryChronologicalComparator.effectiveDateTime(scheduledWithoutDate));
        assertEquals(expected, ClinicalHistoryChronologicalComparator.effectiveDateTime(cancelledWithoutDate));
        assertDoesNotThrow(() -> sorted(completedWithoutOccurredAt, scheduledWithoutDate, cancelledWithoutDate,
                completed(LocalDateTime.of(2026, 1, 1, 0, 0))));
    }

    @Test
    void eventWithEveryDateAndIdNullStillSortsFirstWithoutFailing() {
        var broken = scheduled(LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(broken, "scheduledDate", null);
        ReflectionTestUtils.setField(broken, "createdAt", null);
        var normal = completed(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertEquals(List.of(broken, normal), sorted(normal, broken));
    }
}
