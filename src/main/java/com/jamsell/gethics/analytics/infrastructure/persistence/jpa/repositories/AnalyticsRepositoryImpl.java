package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.repositories.AnalyticsRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private final AnalyticsJpaRepository jpaRepository;

    public AnalyticsRepositoryImpl(AnalyticsJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Analytics> findByOwnerId(UUID ownerId) {
        return jpaRepository.findByOwnerId(ownerId);
    }

    @Override
    public Analytics save(Analytics analytics) {
        return jpaRepository.saveAndFlush(analytics);
    }
}
