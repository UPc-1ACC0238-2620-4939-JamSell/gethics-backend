package com.jamsell.gethics.analytics.domain.model.queries;

import java.util.UUID;

/** US-21: alertas por tendencia ya generadas para un propietario. */
public record GetAlertsByOwnerQuery(UUID ownerId) {
}
