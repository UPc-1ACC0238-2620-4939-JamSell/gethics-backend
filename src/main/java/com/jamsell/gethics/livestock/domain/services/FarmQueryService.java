package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;

import java.util.List;

public interface FarmQueryService {

    /** Granjas del dueno por nombre ascendente. Vacio si no tiene ninguna. */
    List<Farm> handle(GetFarmsByOwnerQuery query);
}
