package com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.veterinary.domain.model.aggregates.VeterinaryAssignment;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeterinaryAssignmentRepository extends JpaRepository<VeterinaryAssignment, UUID> {

    List<VeterinaryAssignment> findByVeterinarianIdAndStatus(UUID veterinarianId,
                                                             VeterinaryAssignmentStatus status);

    boolean existsByVeterinarianIdAndClientIdAndStatus(UUID veterinarianId, UUID clientId,
                                                       VeterinaryAssignmentStatus status);
}
