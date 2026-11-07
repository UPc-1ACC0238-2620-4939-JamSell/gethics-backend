package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.repositories.FarmQueryRepository;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FarmQueryServiceImpl implements FarmQueryService {

    private final FarmQueryRepository repository;

    public FarmQueryServiceImpl(FarmQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Farm> handle(GetFarmsByOwnerQuery query) {
        return repository.findByOwnerId(query.ownerId());
    }
}
