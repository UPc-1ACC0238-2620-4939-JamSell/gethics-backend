package com.jamsell.gethics.veterinary.domain.model.commands;

import java.util.UUID;

public record UnassignClientCommand(UUID assignmentId) {
    public UnassignClientCommand {
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID must be valid");
        }
    }
}
