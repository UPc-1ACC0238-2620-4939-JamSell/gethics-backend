package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

interface ReminderJpaRepository extends JpaRepository<Reminder, UUID> {

    boolean existsBySanitaryEvent_IdAndScheduledFor(UUID sanitaryEventId, LocalDateTime scheduledFor);

    @Query("""
            select r from Reminder r join fetch r.sanitaryEvent e join fetch e.clinicalHistory
            where r.status = com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus.FAILED
              and e.status = com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus.SCHEDULED
              and e.scheduledDate >= :today
            order by r.createdAt asc""")
    List<Reminder> findRetryable(@Param("today") LocalDate today);
}
