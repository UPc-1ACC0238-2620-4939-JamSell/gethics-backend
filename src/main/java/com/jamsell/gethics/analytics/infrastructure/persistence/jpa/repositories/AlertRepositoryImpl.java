package com.jamsell.gethics.analytics.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.valueobjects.AlertStatus;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AlertRepositoryImpl implements AlertRepository {

    private final AlertJpaRepository jpaRepository;

    public AlertRepositoryImpl(AlertJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Alert save(Alert alert) {
        return jpaRepository.saveAndFlush(alert);
    }

    @Override
    public boolean existsByTrendId(UUID trendId) {
        return jpaRepository.existsByTrend_Id(trendId);
    }

    @Override
    public List<Alert> findPending() {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(AlertStatus.PENDING);
    }
}
