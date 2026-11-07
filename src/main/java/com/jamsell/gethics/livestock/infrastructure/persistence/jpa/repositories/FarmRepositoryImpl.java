package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class FarmRepositoryImpl implements FarmRepository {

    private final FarmJpaRepository jpaRepository;

    public FarmRepositoryImpl(FarmJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByOwnerIdAndNormalizedName(UUID ownerId, String normalizedName) {
        return jpaRepository.existsByOwnerIdAndNormalizedName(ownerId, normalizedName);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public Farm save(Farm farm) {
        return jpaRepository.saveAndFlush(farm);
    }
}
