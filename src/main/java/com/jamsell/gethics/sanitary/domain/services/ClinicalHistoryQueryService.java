package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;
import com.jamsell.gethics.sanitary.domain.model.queries.GetCompletedEventsByAnimalsQuery;

import java.util.List;

public interface ClinicalHistoryQueryService {

    /** Eventos del animal en orden cronologico ascendente (mas antiguo primero). Vacio si no hay registros. */
    List<SanitaryEvent> handle(GetClinicalHistoryQuery query);

    /** Eventos COMPLETED de varios animales, en una sola consulta y sin orden garantizado. */
    List<SanitaryEvent> handle(GetCompletedEventsByAnimalsQuery query);
}
