package com.jamsell.gethics.sanitary.infrastructure.notifications;

import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationService;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.VaccinationReminderNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adapter TEMPORAL de {@link NotificationService}: solo escribe en el log. <b>NO representa un push real</b>: no hay
 * proveedor (FCM u otro), credenciales, device tokens ni forma de resolver el propietario del animal (IAM pendiente).
 * Un adapter real deberia reemplazarlo, resolviendo animal -> propietario -> device token.
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
