package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class AnimalFarmAssignmentRepositoryImpl implements AnimalFarmAssignmentRepository {

    private final AnimalFarmAssignmentJpaRepository jpaRepository;

    public AnimalFarmAssignmentRepositoryImpl(AnimalFarmAssignmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AnimalFarmAssignment save(AnimalFarmAssignment assignment) {
        return jpaRepository.saveAndFlush(assignment);
    }

    @Override
    public List<AnimalFarmAssignment> findByAnimalId(UUID animalId) {
        return jpaRepository.findByAnimalIdOrderByAssignedAtAscIdAsc(animalId);
    }
}
