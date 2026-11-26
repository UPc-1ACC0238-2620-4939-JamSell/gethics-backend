package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.queries.GetUpcomingSanitaryEventsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.SanitaryCalendarQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpcomingSanitaryEventsQueryServiceImplTest {

    // 08:00 en Lima: "hoy" segun el Clock es 2042-06-07 (independiente de la fecha real de ejecucion).
    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2042-06-07T13:00:00Z"), ZoneId.of("America/Lima"));

    private final SanitaryCalendarQueryRepository repository = mock(SanitaryCalendarQueryRepository.class);
    private final UpcomingSanitaryEventsQueryServiceImpl service = new UpcomingSanitaryEventsQueryServiceImpl(repository, CLOCK);

    @Test
    void queriesPendingEventsUpToTheLastDayOfTheWindowInclusive() {
        var history = new ClinicalHistory(UUID.randomUUID());
        var overdue = history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.minusDays(2), "Aftosa", TODAY.minusDays(10));
        var soon = history.scheduleEvent(SanitaryEventType.CHECKUP, TODAY.plusDays(1), null, TODAY);
        when(repository.findScheduledBefore(TODAY.plusDays(8))).thenReturn(List.of(overdue, soon));

        var result = service.handle(new GetUpcomingSanitaryEventsQuery(7));

        verify(repository).findScheduledBefore(TODAY.plusDays(8));
        assertEquals(TODAY, result.today());
        assertEquals(7, result.days());
        assertEquals(List.of(overdue, soon), result.events());
    }

    @Test
    void usesTheRequestedWindow() {
        when(repository.findScheduledBefore(TODAY.plusDays(31))).thenReturn(List.of());

        var result = service.handle(new GetUpcomingSanitaryEventsQuery(30));

        verify(repository).findScheduledBefore(TODAY.plusDays(31));
        assertTrue(result.events().isEmpty());
    }
}
