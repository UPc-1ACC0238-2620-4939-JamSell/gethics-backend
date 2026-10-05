package com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    Optional<NotificationPreference> findByUserId(UUID userId);
}
