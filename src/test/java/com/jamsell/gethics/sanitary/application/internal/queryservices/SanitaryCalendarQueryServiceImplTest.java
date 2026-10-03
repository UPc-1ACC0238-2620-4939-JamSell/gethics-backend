package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.exceptions.InvalidCalendarPeriodException;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetSanitaryCalendarQuery;
import com.jamsell.gethics.sanitary.domain.repositories.SanitaryCalendarQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SanitaryCalendarQueryServiceImplTest {

    private final SanitaryCalendarQueryRepository repository = mock(SanitaryCalendarQueryRepository.class);
    private final SanitaryCalendarQueryServiceImpl service = new SanitaryCalendarQueryServiceImpl(repository);

    private void assertRange(int year, int month, LocalDate from, LocalDate toExclusive) {
        List<SanitaryEvent> expected = List.of();
        when(repository.findScheduledBetween(from, toExclusive)).thenReturn(expected);

        assertSame(expected, service.handle(GetSanitaryCalendarQuery.of(year, month)));

        verify(repository).findScheduledBetween(from, toExclusive);
    }

    @Test
    void queriesFromFirstDayOfMonthToFirstDayOfNextMonth() {
        assertRange(2026, 10, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
    }

    @Test
    void decemberEndsOnJanuaryOfNextYear() {
        assertRange(2026, 12, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1));
    }

    @Test
    void decemberOfTheLastSupportedYearComputesItsRangeWithoutOverflow() {
        assertRange(5_874_896, 12, LocalDate.of(5_874_896, 12, 1), LocalDate.of(5_874_897, 1, 1));
    }

    @Test
    void firstSupportedPeriodIsQueried() {
        assertRange(-4712, 1, LocalDate.of(-4712, 1, 1), LocalDate.of(-4712, 2, 1));
    }

    @Test
    void periodsBeyondThePersistenceDateRangeAreRejectedBeforeReachingTheRepository() {
        for (var period : List.of(
                GetSanitaryCalendarQuery.of(9_999_999, 1),
                GetSanitaryCalendarQuery.of(5_874_897, 1),
                GetSanitaryCalendarQuery.of(999_999_999, 12),
                GetSanitaryCalendarQuery.of(-4713, 12),
                GetSanitaryCalendarQuery.of(-999_999_999, 1))) {
            assertThrows(InvalidCalendarPeriodException.class, () -> service.handle(period));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void februaryRespectsLeapYears() {
        assertEquals(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 2, 1).plusMonths(1));
        assertRange(2028, 2, LocalDate.of(2028, 2, 1), LocalDate.of(2028, 3, 1));
        assertRange(2027, 2, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 1));
    }
}
