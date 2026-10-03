package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ClinicalHistoryQueryRepositoryImpl implements ClinicalHistoryQueryRepository {

    private final SanitaryEventJpaRepository jpaRepository;

    public ClinicalHistoryQueryRepositoryImpl(SanitaryEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<SanitaryEvent> findEventsByAnimalId(UUID animalId) {
        return jpaRepository.findByClinicalHistory_AnimalId(animalId);
    }
}
