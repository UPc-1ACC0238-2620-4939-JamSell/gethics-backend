package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface SanitaryEventJpaRepository extends JpaRepository<SanitaryEvent, UUID> {

    @Query("""
            select e from SanitaryEvent e join fetch e.clinicalHistory
            where e.status = com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus.SCHEDULED
              and e.scheduledDate >= :from and e.scheduledDate < :toExclusive
            order by e.scheduledDate asc, e.createdAt asc""")
    List<SanitaryEvent> findScheduledBetween(@Param("from") LocalDate from, @Param("toExclusive") LocalDate toExclusive);
}
