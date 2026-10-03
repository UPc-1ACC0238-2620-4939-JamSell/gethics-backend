package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderCandidateQueryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReminderCandidateQueryRepositoryImpl implements ReminderCandidateQueryRepository {

    private final SanitaryEventJpaRepository jpaRepository;

    public ReminderCandidateQueryRepositoryImpl(SanitaryEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SanitaryEvent> findVaccinationsAwaitingReminder(LocalDate eventDate, LocalDateTime reminderTime) {
        return jpaRepository.findVaccinationsAwaitingReminder(eventDate, reminderTime);
    }
}
