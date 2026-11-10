package com.jamsell.gethics.livestock.domain.model.commands;

import java.util.UUID;

/** US-08: baja logica de un animal. No borra el registro ni su historial. */
public record DeactivateAnimalCommand(UUID animalId) {
}
