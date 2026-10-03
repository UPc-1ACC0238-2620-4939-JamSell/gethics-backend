package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ReminderRepositoryImpl implements ReminderRepository {

    private final ReminderJpaRepository jpaRepository;

    public ReminderRepositoryImpl(ReminderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Reminder> createIfAbsent(Reminder reminder) {
        if (exists(reminder)) {
            return Optional.empty();
        }
        try {
            return Optional.of(jpaRepository.saveAndFlush(reminder));
        } catch (DataIntegrityViolationException e) {
            // Carrera con otra ejecucion: solo se absorbe si la causa es realmente el duplicado; cualquier otra se propaga.
            if (exists(reminder)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public Reminder save(Reminder reminder) {
        return jpaRepository.saveAndFlush(reminder);
    }

    @Override
    public List<Reminder> findRetryable(LocalDate today) {
        return jpaRepository.findRetryable(today);
    }

    private boolean exists(Reminder reminder) {
        return jpaRepository.existsBySanitaryEvent_IdAndScheduledFor(reminder.getSanitaryEventId(), reminder.getScheduledFor());
    }
}
