package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.repositories.FarmQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class FarmQueryRepositoryImpl implements FarmQueryRepository {

    private final FarmJpaRepository jpaRepository;

    public FarmQueryRepositoryImpl(FarmJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Farm> findByOwnerId(UUID ownerId) {
        return jpaRepository.findByOwnerIdOrderByNormalizedNameAsc(ownerId);
    }
}
