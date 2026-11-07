package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FarmJpaRepository extends JpaRepository<Farm, UUID> {

    boolean existsByOwnerIdAndNormalizedName(UUID ownerId, String normalizedName);

    List<Farm> findByOwnerIdOrderByNormalizedNameAsc(UUID ownerId);
}
