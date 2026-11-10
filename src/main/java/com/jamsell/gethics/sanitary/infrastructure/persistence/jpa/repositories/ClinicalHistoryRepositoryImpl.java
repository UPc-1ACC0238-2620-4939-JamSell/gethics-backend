package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ClinicalHistoryRepositoryImpl implements ClinicalHistoryRepository {

    private final ClinicalHistoryJpaRepository jpaRepository;
    private final SanitaryEventJpaRepository eventJpaRepository;

    public ClinicalHistoryRepositoryImpl(ClinicalHistoryJpaRepository jpaRepository,
                                         SanitaryEventJpaRepository eventJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.eventJpaRepository = eventJpaRepository;
    }

    @Override
    public Optional<ClinicalHistory> findByAnimalId(UUID animalId) {
        return jpaRepository.findByAnimalId(animalId);
    }

    @Override
    public ClinicalHistory save(ClinicalHistory clinicalHistory) {
        return jpaRepository.saveAndFlush(clinicalHistory);
    }

    @Override
    public Optional<SanitaryEvent> findEventByClientRequestId(UUID clientRequestId) {
        return eventJpaRepository.findByClientRequestId(clientRequestId);
    }
}
