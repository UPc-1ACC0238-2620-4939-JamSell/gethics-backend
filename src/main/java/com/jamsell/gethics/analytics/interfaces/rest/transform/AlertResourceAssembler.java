package com.jamsell.gethics.analytics.interfaces.rest.transform;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.interfaces.rest.resources.AlertListResource;
import com.jamsell.gethics.analytics.interfaces.rest.resources.AlertResource;

import java.util.List;

public final class AlertResourceAssembler {

    static final String NO_ALERTS = "No hay alertas registradas.";

    private AlertResourceAssembler() {
    }

    public static AlertResource toResourceFromEntity(Alert alert) {
        var trend = alert.getTrend();
        return new AlertResource(alert.getId(), trend.getId(), trend.getType(), trend.getRiskLevel(),
                alert.getMessage(), alert.getStatus(), alert.getCreatedAt());
    }

    public static AlertListResource toListResource(List<Alert> alerts) {
        var items = alerts.stream().map(AlertResourceAssembler::toResourceFromEntity).toList();
        return new AlertListResource(items, items.isEmpty() ? NO_ALERTS : null);
    }
}
