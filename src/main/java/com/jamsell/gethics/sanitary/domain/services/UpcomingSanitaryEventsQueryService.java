package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.queries.GetUpcomingSanitaryEventsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.UpcomingSanitaryEvents;

public interface UpcomingSanitaryEventsQueryService {
    UpcomingSanitaryEvents handle(GetUpcomingSanitaryEventsQuery query);
}
