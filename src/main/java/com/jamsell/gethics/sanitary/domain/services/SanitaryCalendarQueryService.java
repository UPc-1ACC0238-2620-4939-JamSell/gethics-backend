package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetSanitaryCalendarQuery;

import java.util.List;

public interface SanitaryCalendarQueryService {
    List<SanitaryEvent> handle(GetSanitaryCalendarQuery query);
}
