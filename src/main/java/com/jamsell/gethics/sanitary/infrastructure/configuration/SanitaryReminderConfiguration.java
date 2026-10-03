package com.jamsell.gethics.sanitary.infrastructure.configuration;

import com.jamsell.gethics.sanitary.domain.services.SanitaryScheduleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class SanitaryReminderConfiguration {

    /**
     * Clock de la zona configurada en {@code gethics.sanitary.reminders.zone}, la MISMA que usa el cron del job, de modo
     * que "hoy" para el calculo de "faltan 3 dias" coincide con la zona en que se programa la ejecucion. Hora y zona son
     * valores tecnicos por defecto, no reglas de negocio.
     */
    @Bean
    public Clock clock(@Value("${gethics.sanitary.reminders.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public SanitaryScheduleService sanitaryScheduleService() {
        return new SanitaryScheduleService();
    }
}
