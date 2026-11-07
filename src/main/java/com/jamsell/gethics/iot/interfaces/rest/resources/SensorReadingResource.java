package com.jamsell.gethics.iot.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SensorReadingResource(UUID id, String metric, BigDecimal value, LocalDateTime recordedAt) {
}
