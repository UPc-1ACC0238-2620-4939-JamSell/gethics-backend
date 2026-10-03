package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ClinicalHistoryRepositoryImpl implements ClinicalHistoryRepository {

    private final ClinicalHistoryJpaRepository jpaRepository;

    public ClinicalHistoryRepositoryImpl(ClinicalHistoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ClinicalHistory> findByAnimalId(UUID animalId) {
        return jpaRepository.findByAnimalId(animalId);
    }

    @Override
    public ClinicalHistory save(ClinicalHistory clinicalHistory) {
        return jpaRepository.saveAndFlush(clinicalHistory);
    }
}
