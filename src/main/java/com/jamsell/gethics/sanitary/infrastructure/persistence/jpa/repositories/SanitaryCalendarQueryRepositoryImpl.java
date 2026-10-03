package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.repositories.SanitaryCalendarQueryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class SanitaryCalendarQueryRepositoryImpl implements SanitaryCalendarQueryRepository {

    private final SanitaryEventJpaRepository jpaRepository;

    public SanitaryCalendarQueryRepositoryImpl(SanitaryEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SanitaryEvent> findScheduledBetween(LocalDate from, LocalDate toExclusive) {
        return jpaRepository.findScheduledBetween(from, toExclusive);
    }
}
