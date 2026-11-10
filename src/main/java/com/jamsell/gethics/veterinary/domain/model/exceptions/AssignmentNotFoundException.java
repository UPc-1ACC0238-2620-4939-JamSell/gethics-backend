package com.jamsell.gethics.veterinary.domain.model.exceptions;

import java.util.UUID;

public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(UUID assignmentId) {
        super("Assignment " + assignmentId + " not found or already unassigned");
    }
}
