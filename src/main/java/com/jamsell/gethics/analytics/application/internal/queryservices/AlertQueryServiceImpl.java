package com.jamsell.gethics.analytics.application.internal.queryservices;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.queries.GetAlertsByOwnerQuery;
import com.jamsell.gethics.analytics.domain.repositories.AlertRepository;
import com.jamsell.gethics.analytics.domain.services.AlertQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * US-21: expone las alertas ya persistidas por el mecanismo de analisis. No detecta ni clasifica tendencias.
 */
@Service
public class AlertQueryServiceImpl implements AlertQueryService {

    private final AlertRepository alertRepository;

    public AlertQueryServiceImpl(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> handle(GetAlertsByOwnerQuery query) {
        return alertRepository.findByOwnerId(query.ownerId());
    }
}
