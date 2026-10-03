package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AnalyticsJpaRepository extends JpaRepository<Analytics, UUID> {
    Optional<Analytics> findByOwnerId(UUID ownerId);
}
