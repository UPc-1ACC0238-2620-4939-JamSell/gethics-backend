package com.jamsell.gethics.analytics.domain.services;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.queries.GetAlertsByOwnerQuery;

import java.util.List;

public interface AlertQueryService {

    /** Alertas del propietario de la mas reciente a la mas antigua. Vacio si no tiene ninguna. */
    List<Alert> handle(GetAlertsByOwnerQuery query);
}
