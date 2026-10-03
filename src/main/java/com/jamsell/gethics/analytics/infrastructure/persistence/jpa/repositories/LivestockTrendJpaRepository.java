package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.entities.LivestockTrend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface LivestockTrendJpaRepository extends JpaRepository<LivestockTrend, UUID> {
}
