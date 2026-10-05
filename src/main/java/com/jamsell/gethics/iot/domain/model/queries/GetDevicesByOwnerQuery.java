package com.jamsell.gethics.iot.domain.model.queries;

import java.util.UUID;

public record GetDevicesByOwnerQuery(UUID ownerId) {
}
