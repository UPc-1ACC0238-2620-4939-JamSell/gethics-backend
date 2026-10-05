package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;

public interface AnimalQueryService {
    Animal handle(GetAnimalByIdQuery query);
}
