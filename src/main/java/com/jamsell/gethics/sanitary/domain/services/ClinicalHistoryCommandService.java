package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

public interface ClinicalHistoryCommandService {
    SanitaryEvent handle(RegisterSanitaryEventCommand command);

    SanitaryEvent handle(ScheduleSanitaryEventCommand command);

    SanitaryEvent handle(CompleteScheduledEventCommand command);
}
