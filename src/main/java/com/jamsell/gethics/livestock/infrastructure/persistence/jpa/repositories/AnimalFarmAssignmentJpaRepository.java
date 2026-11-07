package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AnimalFarmAssignmentJpaRepository extends JpaRepository<AnimalFarmAssignment, UUID> {

    List<AnimalFarmAssignment> findByAnimalIdOrderByAssignedAtAscIdAsc(UUID animalId);
}
