package com.jamsell.gethics.sanitary.domain.model.queries;

import com.jamsell.gethics.sanitary.domain.exceptions.InvalidUpcomingWindowException;

/** US-46: eventos programados que vencen dentro de los proximos {@code days} dias, mas los ya vencidos. */
public record GetUpcomingSanitaryEventsQuery(int days) {

    public static final int DEFAULT_DAYS = 7;
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 30;

    public GetUpcomingSanitaryEventsQuery {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new InvalidUpcomingWindowException();
        }
    }
}
