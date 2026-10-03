package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationDeliveryException;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationService;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.VaccinationReminderNotification;
import com.jamsell.gethics.sanitary.domain.model.commands.ProcessVaccinationRemindersCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderCandidateQueryRepository;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderRepository;
import com.jamsell.gethics.sanitary.domain.services.SanitaryScheduleService;
import com.jamsell.gethics.sanitary.domain.services.VaccinationReminderCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Orquesta la revision diaria de recordatorios de vacunacion: reclama (persiste) el recordatorio, notifica y registra
 * el resultado. Garantia de entrega: <b>a lo sumo una vez en el camino normal</b>, no "exactly once".
 * <ul>
 *   <li>El recordatorio se confirma en la base ANTES de notificar y solo quien lo reclama notifica.</li>
 *   <li>Un fallo conocido de entrega deja el recordatorio FAILED y se reintenta en ejecuciones posteriores.</li>
 *   <li>Si el proceso cae entre el reclamo y el envio queda un PENDING huerfano que NO se reintenta (puede perderse un
 *       aviso, pero nunca se duplica).</li>
 * </ul>
 * Deliberadamente NO es {@code @Transactional}: cada operacion del repositorio confirma por separado, de modo que el
 * reclamo queda persistido antes del envio y el envio no ocurre dentro de una transaccion de base de datos.
 */
@Slf4j
@Service
public class VaccinationReminderCommandServiceImpl implements VaccinationReminderCommandService {

    private final ReminderCandidateQueryRepository candidates;
    private final ReminderRepository reminders;
    private final SanitaryScheduleService scheduleService;
    private final NotificationService notifications;
    private final Clock clock;

    public VaccinationReminderCommandServiceImpl(ReminderCandidateQueryRepository candidates, ReminderRepository reminders,
                                                 SanitaryScheduleService scheduleService, NotificationService notifications,
                                                 Clock clock) {
        this.candidates = candidates;
        this.reminders = reminders;
        this.scheduleService = scheduleService;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public void handle(ProcessVaccinationRemindersCommand command) {
        var today = LocalDate.now(clock);
        var eventDate = scheduleService.eventDateToRemind(today);
        var reminderTime = scheduleService.reminderTimeFor(eventDate);

        // Se leen ANTES de procesar los nuevos: un recordatorio que falla en esta ejecucion no se reintenta en ella misma.
        var retryable = reminders.findRetryable(today);

        int created = 0;
        for (var event : candidates.findVaccinationsAwaitingReminder(eventDate, reminderTime)) {
            if (!scheduleService.needsReminder(event, today)) {
                continue;
            }
            var claimed = reminders.createIfAbsent(scheduleService.generateReminder(event));
            if (claimed.isEmpty()) {
                log.debug("Recordatorio ya reclamado para el evento {}", event.getId());
                continue;
            }
            created++;
            deliver(claimed.get());
        }

        int retried = 0;
        for (var reminder : retryable) {
            if (scheduleService.canRetry(reminder, today)) {
                retried++;
                deliver(reminder);
            }
        }
        log.info("Recordatorios de vacunacion para {}: {} nuevos, {} reintentos", eventDate, created, retried);
    }

    private void deliver(Reminder reminder) {
        var event = reminder.getSanitaryEvent();
        var notification = new VaccinationReminderNotification(event.getId(), event.getClinicalHistory().getAnimalId(),
                event.getScheduledDate(), event.getDescription());
        try {
            notifications.sendVaccinationReminder(notification);
            reminder.markSent(clock.instant());
        } catch (NotificationDeliveryException e) {
            log.warn("Fallo el envio del recordatorio del evento {}: {}", event.getId(), e.getMessage());
            reminder.markFailed();
        }
        reminders.save(reminder);
    }
}
