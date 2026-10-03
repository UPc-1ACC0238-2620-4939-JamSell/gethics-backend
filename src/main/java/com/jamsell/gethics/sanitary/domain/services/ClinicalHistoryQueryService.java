package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;

import java.util.List;

public interface ClinicalHistoryQueryService {

    /** Eventos del animal en orden cronologico ascendente (mas antiguo primero). Vacio si no hay registros. */
    List<SanitaryEvent> handle(GetClinicalHistoryQuery query);
}
