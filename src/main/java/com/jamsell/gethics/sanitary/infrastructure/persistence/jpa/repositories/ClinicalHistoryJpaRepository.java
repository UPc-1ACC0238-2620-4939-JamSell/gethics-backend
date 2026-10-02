package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ClinicalHistoryJpaRepository extends JpaRepository<ClinicalHistory, UUID> {
    Optional<ClinicalHistory> findByAnimalId(UUID animalId);
}
