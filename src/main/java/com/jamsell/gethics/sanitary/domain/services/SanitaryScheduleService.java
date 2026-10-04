package com.jamsell.gethics.sanitary.domain.services;

import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain Service: decide si un evento sanitario requiere recordatorio y lo genera. Es la autoridad de la regla; las
 * consultas de los repositorios solo preseleccionan candidatos. La orquestacion (persistir, notificar, reintentar) es de
 * la capa Application.
 * <p>
 * US-13, Escenario 2: una vacuna se considera "aplicada" cuando su MISMO evento pasa SCHEDULED -> COMPLETED
 * ({@code POST .../sanitary-events/{eventId}/complete}); desde entonces no genera recordatorio ni reintenta uno FAILED.
 * No se deduce por animal/tipo/fecha que un evento COMPLETED registrado aparte por US-11 corresponda a uno programado.
 */
public class SanitaryScheduleService {

    public static final int REMINDER_DAYS_BEFORE = 3;

    /** Fecha de evento que hoy requiere recordatorio: hoy + {@value #REMINDER_DAYS_BEFORE} dias. */
    public LocalDate eventDateToRemind(LocalDate today) {
        return today.plusDays(REMINDER_DAYS_BEFORE);
    }

    /** Valor de {@code scheduledFor}: inicio del dia del aviso. Es determinista, por eso sirve de clave de idempotencia. */
    public LocalDateTime reminderTimeFor(LocalDate eventDate) {
        return eventDate.minusDays(REMINDER_DAYS_BEFORE).atStartOfDay();
    }

    public boolean needsReminder(SanitaryEvent event, LocalDate today) {
        return event.getType() == SanitaryEventType.VACCINATION
                && event.getStatus() == SanitaryEventStatus.SCHEDULED
                && eventDateToRemind(today).equals(event.getScheduledDate());
    }

    public Reminder generateReminder(SanitaryEvent event) {
        Objects.requireNonNull(event.getScheduledDate(), "scheduledDate");
        return Reminder.create(event, reminderTimeFor(event.getScheduledDate()));
    }

    /** Un FAILED se reintenta mientras el evento siga SCHEDULED y su fecha no haya pasado. */
    public boolean canRetry(Reminder reminder, LocalDate today) {
        var event = reminder.getSanitaryEvent();
        return reminder.getStatus() == ReminderStatus.FAILED
                && event.getStatus() == SanitaryEventStatus.SCHEDULED
                && event.getScheduledDate() != null
                && !event.getScheduledDate().isBefore(today);
    }
}
