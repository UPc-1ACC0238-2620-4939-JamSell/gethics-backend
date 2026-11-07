package com.jamsell.gethics.iot.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ingesta de una lectura del sensor para el dispositivo con {@code code} (ya vinculado). {@code recordedAt} es
 * opcional: lo reporta el propio sensor; si no viene, se usa la hora de recepcion del servidor.
 */
public record RegisterSensorReadingCommand(String code, String metric, BigDecimal value, LocalDateTime recordedAt) {
}
