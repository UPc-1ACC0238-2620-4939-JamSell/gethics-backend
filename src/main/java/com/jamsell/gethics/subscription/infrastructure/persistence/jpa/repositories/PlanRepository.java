package com.jamsell.gethics.subscription.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.subscription.domain.model.aggregates.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);

    List<Plan> findAllByOrderByPriceAsc();
}
