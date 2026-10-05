package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AnimalJpaRepository extends JpaRepository<Animal, UUID> {

    boolean existsByTag(String tag);
}
