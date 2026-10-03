package com.jamsell.gethics.sanitary.interfaces.scheduling;

import com.jamsell.gethics.sanitary.domain.model.commands.ProcessVaccinationRemindersCommand;
import com.jamsell.gethics.sanitary.domain.services.VaccinationReminderCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Disparador diario (adapter de entrada): solo delega en el servicio de aplicacion. */
@Component
public class VaccinationReminderJob {

    private final VaccinationReminderCommandService commandService;

    public VaccinationReminderJob(VaccinationReminderCommandService commandService) {
        this.commandService = commandService;
    }

    @Scheduled(cron = "${gethics.sanitary.reminders.cron}", zone = "${gethics.sanitary.reminders.zone}")
    public void run() {
        commandService.handle(new ProcessVaccinationRemindersCommand());
    }
}
