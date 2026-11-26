package com.jamsell.gethics.sanitary.domain.model.queries;

import com.jamsell.gethics.sanitary.domain.exceptions.InvalidUpcomingWindowException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUpcomingSanitaryEventsQueryTest {

    @Test
    void acceptsTheLimitsOfTheAlertWindow() {
        assertEquals(1, new GetUpcomingSanitaryEventsQuery(1).days());
        assertEquals(30, new GetUpcomingSanitaryEventsQuery(30).days());
        assertEquals(7, new GetUpcomingSanitaryEventsQuery(GetUpcomingSanitaryEventsQuery.DEFAULT_DAYS).days());
    }

    @Test
    void rejectsWindowsOutsideOneToThirtyDays() {
        var ex = assertThrows(InvalidUpcomingWindowException.class, () -> new GetUpcomingSanitaryEventsQuery(0));
        assertEquals("La ventana de alerta debe estar entre 1 y 30 dias.", ex.getMessage());
        assertThrows(InvalidUpcomingWindowException.class, () -> new GetUpcomingSanitaryEventsQuery(31));
        assertThrows(InvalidUpcomingWindowException.class, () -> new GetUpcomingSanitaryEventsQuery(-5));
    }
}
