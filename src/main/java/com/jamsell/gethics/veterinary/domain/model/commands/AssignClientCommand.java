package com.jamsell.gethics.veterinary.domain.model.commands;

import java.util.UUID;

public record AssignClientCommand(UUID veterinarianId, UUID clientId) {
}

