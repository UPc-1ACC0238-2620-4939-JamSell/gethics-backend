package com.jamsell.gethics.sanitary.domain.model.queries;

import com.jamsell.gethics.sanitary.domain.exceptions.InvalidCalendarPeriodException;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetSanitaryCalendarQueryTest {

    @Test
    void buildsTheYearMonthPeriod() {
        assertEquals(YearMonth.of(2026, 10), GetSanitaryCalendarQuery.of(2026, 10).period());
    }

    @Test
    void rejectsInvalidMonths() {
        assertThrows(InvalidCalendarPeriodException.class, () -> GetSanitaryCalendarQuery.of(2026, 0));
        assertThrows(InvalidCalendarPeriodException.class, () -> GetSanitaryCalendarQuery.of(2026, 13));
    }

    @Test
    void rejectsYearsYearMonthCannotRepresent() {
        assertThrows(InvalidCalendarPeriodException.class, () -> GetSanitaryCalendarQuery.of(Integer.MAX_VALUE, 1));
        assertThrows(InvalidCalendarPeriodException.class, () -> GetSanitaryCalendarQuery.of(Integer.MIN_VALUE, 1));
    }
}
