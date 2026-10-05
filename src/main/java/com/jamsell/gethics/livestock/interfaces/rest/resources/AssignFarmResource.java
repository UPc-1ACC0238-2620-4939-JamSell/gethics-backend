package com.jamsell.gethics.livestock.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignFarmResource(@NotNull(message = "La granja es obligatoria.") UUID farmId) {
}
