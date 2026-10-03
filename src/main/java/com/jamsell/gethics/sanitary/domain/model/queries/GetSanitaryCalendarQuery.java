package com.jamsell.gethics.sanitary.domain.model.queries;

import com.jamsell.gethics.sanitary.domain.exceptions.InvalidCalendarPeriodException;

import java.time.DateTimeException;
import java.time.YearMonth;

public record GetSanitaryCalendarQuery(YearMonth period) {

    public static GetSanitaryCalendarQuery of(int year, int month) {
        try {
            return new GetSanitaryCalendarQuery(YearMonth.of(year, month));
        } catch (DateTimeException e) {
            throw new InvalidCalendarPeriodException();
        }
    }
}
