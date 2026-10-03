package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AlertJpaRepository extends JpaRepository<Alert, UUID> {

    boolean existsByTrend_Id(UUID trendId);

    List<Alert> findByStatusOrderByCreatedAtAsc(AlertStatus status);
}
