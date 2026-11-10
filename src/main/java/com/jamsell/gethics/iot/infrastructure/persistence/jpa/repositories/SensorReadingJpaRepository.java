package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SensorReadingJpaRepository extends JpaRepository<SensorReading, UUID> {

    Page<SensorReading> findByDeviceId(UUID deviceId, Pageable pageable);
}
