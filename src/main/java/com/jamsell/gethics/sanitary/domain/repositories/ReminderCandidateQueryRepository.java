package com.jamsell.gethics.sanitary.domain.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Puerto de lectura: candidatos a recordatorio entre los eventos de todos los historiales. */
public interface ReminderCandidateQueryRepository {

    /**
     * Preseleccion: eventos VACCINATION y SCHEDULED con {@code scheduledDate = eventDate} que aun no tienen un
     * recordatorio con {@code scheduledFor = reminderTime}; con su historial ya cargado.
     */
    List<SanitaryEvent> findVaccinationsAwaitingReminder(LocalDate eventDate, LocalDateTime reminderTime);
}
