package com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.finance.domain.model.entities.FinancialTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {

    List<FinancialTransaction> findByFinancialManagementIdOrderByOccurredOnDesc(UUID financialManagementId);
}
