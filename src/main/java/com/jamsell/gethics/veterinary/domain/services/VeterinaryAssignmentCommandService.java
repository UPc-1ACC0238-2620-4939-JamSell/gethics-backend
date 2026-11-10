package com.jamsell.gethics.veterinary.domain.services;

import com.jamsell.gethics.veterinary.domain.model.aggregates.VeterinaryAssignment;
import com.jamsell.gethics.veterinary.domain.model.commands.AssignClientCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.UnassignClientCommand;

public interface VeterinaryAssignmentCommandService {

    VeterinaryAssignment handle(AssignClientCommand command);

    void handle(UnassignClientCommand command);

}