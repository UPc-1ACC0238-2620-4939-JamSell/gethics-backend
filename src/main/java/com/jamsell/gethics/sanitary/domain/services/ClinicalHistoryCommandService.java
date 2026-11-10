package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.commands.CancelScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventCreation;

public interface ClinicalHistoryCommandService {
    /** US-35: {@code created} = false si un reintento con el mismo clientRequestId devolvio el evento existente. */
    SanitaryEventCreation handle(RegisterSanitaryEventCommand command);

    SanitaryEventCreation handle(ScheduleSanitaryEventCommand command);

    SanitaryEvent handle(CompleteScheduledEventCommand command);

    SanitaryEvent handle(CancelScheduledEventCommand command);
}
