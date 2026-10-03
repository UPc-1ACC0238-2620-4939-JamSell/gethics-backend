package com.jamsell.gethics.sanitary.interfaces.scheduling;

import com.jamsell.gethics.sanitary.domain.model.commands.ProcessVaccinationRemindersCommand;
import com.jamsell.gethics.sanitary.domain.services.VaccinationReminderCommandService;
import com.jamsell.gethics.sanitary.infrastructure.configuration.SanitaryReminderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VaccinationReminderJobTest {

    @Test
    void delegatesToTheCommandService() {
        var service = mock(VaccinationReminderCommandService.class);

        new VaccinationReminderJob(service).run();

        verify(service).handle(new ProcessVaccinationRemindersCommand());
    }

    @Test
    void cronAndZoneComeFromTheSameConfigurationProperties() throws Exception {
        var scheduled = VaccinationReminderJob.class.getMethod("run").getAnnotation(Scheduled.class);

        assertEquals("${gethics.sanitary.reminders.cron}", scheduled.cron());
        assertEquals("${gethics.sanitary.reminders.zone}", scheduled.zone());
    }

    @Test
    void clockUsesTheConfiguredZone() {
        var clock = new SanitaryReminderConfiguration().clock("America/Lima");

        assertEquals(ZoneId.of("America/Lima"), clock.getZone());
    }
}
