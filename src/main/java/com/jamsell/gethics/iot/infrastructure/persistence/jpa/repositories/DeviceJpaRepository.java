package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeviceJpaRepository extends JpaRepository<Device, UUID> {

    boolean existsByCode(String code);

    Optional<Device> findByCode(String code);

    List<Device> findByOwnerId(UUID ownerId);

    List<Device> findByStatus(DeviceStatus status);
}
