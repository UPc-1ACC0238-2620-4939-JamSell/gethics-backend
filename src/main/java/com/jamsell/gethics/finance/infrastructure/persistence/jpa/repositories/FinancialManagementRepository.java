package com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialManagementRepository extends JpaRepository<FinancialManagement, UUID> {

    Optional<FinancialManagement> findByOwnerId(UUID ownerId);
}
