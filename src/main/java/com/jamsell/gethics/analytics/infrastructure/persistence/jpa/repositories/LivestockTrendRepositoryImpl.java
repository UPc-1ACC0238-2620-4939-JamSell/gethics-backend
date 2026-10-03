package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import com.jamsell.gethics.analytics.domain.repositories.LivestockTrendRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LivestockTrendRepositoryImpl implements LivestockTrendRepository {

    private final LivestockTrendJpaRepository jpaRepository;

    public LivestockTrendRepositoryImpl(LivestockTrendJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<LivestockTrend> findById(UUID trendId) {
        return jpaRepository.findById(trendId);
    }
}
