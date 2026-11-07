package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimalQueryServiceImpl implements AnimalQueryService {

    private final AnimalQueryRepository repository;

    public AnimalQueryServiceImpl(AnimalQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Animal> handle(GetAnimalsQuery query) {
        return repository.search(query.search(), query.status());
    }
}
