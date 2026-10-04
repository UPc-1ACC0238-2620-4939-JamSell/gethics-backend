package com.jamsell.gethics.sanitary.infrastructure.notifications;

import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationService;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.VaccinationReminderNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter TEMPORAL de {@link NotificationService}: solo escribe en el log. <b>NO representa un push real</b>.
 * Bloqueos externos: Livestock no existe (no se puede resolver animal -> propietario), IAM no modela device tokens y no
 * hay proveedor push (FCM u otro) ni credenciales. Un adapter real deberia reemplazarlo, resolviendo
 * animal -> propietario -> device token.
 */
@Slf4j
@Component
public class LoggingNotificationService implements NotificationService {

    @Override
    public void sendVaccinationReminder(VaccinationReminderNotification n) {
        log.info("[SIMULADO - no es un push real] Recordatorio de vacunacion: animal={}, evento={}, fecha={}, descripcion={}",
                n.animalId(), n.sanitaryEventId(), n.vaccinationDate(), n.description());
    }
}
