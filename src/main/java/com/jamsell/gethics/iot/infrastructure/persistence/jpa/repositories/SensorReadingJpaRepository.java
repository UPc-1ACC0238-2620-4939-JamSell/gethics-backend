package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// Specification: el filtro de fechas es opcional en cada extremo y se resuelve en el WHERE de la consulta.
interface SensorReadingJpaRepository extends JpaRepository<SensorReading, UUID>,
        JpaSpecificationExecutor<SensorReading> {
}
