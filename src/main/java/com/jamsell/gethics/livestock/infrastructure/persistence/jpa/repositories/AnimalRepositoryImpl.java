package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AnimalRepositoryImpl implements AnimalRepository {

    private final AnimalJpaRepository jpaRepository;

    public AnimalRepositoryImpl(AnimalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByTag(String tag) {
        return jpaRepository.existsByTag(tag);
    }

    @Override
    public Animal save(Animal animal) {
        return jpaRepository.saveAndFlush(animal);
    }
}
