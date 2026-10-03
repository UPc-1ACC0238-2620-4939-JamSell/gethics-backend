package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.commands.ProcessVaccinationRemindersCommand;

public interface VaccinationReminderCommandService {
    void handle(ProcessVaccinationRemindersCommand command);
}
