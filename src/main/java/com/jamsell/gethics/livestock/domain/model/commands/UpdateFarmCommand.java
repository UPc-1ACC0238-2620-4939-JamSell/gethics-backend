package com.jamsell.gethics.livestock.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;

/** US-32: edicion de una granja (reemplazo completo de nombre, ubicacion y tamano; tamano null = sin registrar). */
public record UpdateFarmCommand(UUID farmId, String name, String location, BigDecimal sizeHectares) {
}
