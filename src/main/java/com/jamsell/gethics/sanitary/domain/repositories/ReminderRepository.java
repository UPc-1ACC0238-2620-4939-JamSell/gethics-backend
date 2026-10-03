package com.jamsell.gethics.sanitary.domain.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReminderRepository {

    /**
     * Persiste y confirma el recordatorio salvo que ya exista uno para el mismo evento y {@code scheduledFor}
     * (en cuyo caso devuelve vacio). Es el "reclamo" que garantiza que solo una ejecucion envie la notificacion.
     */
    Optional<Reminder> createIfAbsent(Reminder reminder);

    Reminder save(Reminder reminder);

    /** Preseleccion: recordatorios FAILED cuyo evento sigue SCHEDULED y con fecha no anterior a {@code today}, con evento e historial cargados. */
    List<Reminder> findRetryable(LocalDate today);
}
