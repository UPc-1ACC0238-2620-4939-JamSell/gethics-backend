package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.model.queries.GetUpcomingSanitaryEventsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.UpcomingSanitaryEvents;
import com.jamsell.gethics.sanitary.domain.repositories.SanitaryCalendarQueryRepository;
import com.jamsell.gethics.sanitary.domain.services.UpcomingSanitaryEventsQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * US-46: eventos pendientes cuya fecha programada cae entre hoy y hoy + days (inclusive), mas los vencidos que siguen
 * SCHEDULED. "Hoy" sale del mismo Clock que usan la programacion y los recordatorios.
 */
@Service
public class UpcomingSanitaryEventsQueryServiceImpl implements UpcomingSanitaryEventsQueryService {

    private final SanitaryCalendarQueryRepository repository;
    private final Clock clock;

    public UpcomingSanitaryEventsQueryServiceImpl(SanitaryCalendarQueryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UpcomingSanitaryEvents handle(GetUpcomingSanitaryEventsQuery query) {
        var today = LocalDate.now(clock);
        var events = repository.findScheduledBefore(today.plusDays(query.days() + 1L));
        return new UpcomingSanitaryEvents(today, query.days(), events);
    }
}
