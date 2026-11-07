package com.jamsell.gethics.livestock.domain.model.commands;

import java.util.UUID;

public record AssignAnimalToFarmCommand(UUID animalId, UUID farmId) {
}
