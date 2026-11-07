package com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CareRecordRepository extends JpaRepository<CareRecord, UUID> {

    Optional<CareRecord> findByClientRequestId(UUID clientRequestId);
}
