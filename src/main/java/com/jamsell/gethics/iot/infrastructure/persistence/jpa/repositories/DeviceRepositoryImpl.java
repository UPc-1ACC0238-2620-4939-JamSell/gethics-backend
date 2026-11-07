package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

    private final DeviceJpaRepository jpaRepository;

    public DeviceRepositoryImpl(DeviceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public Optional<Device> findByCode(String code) {
        return jpaRepository.findByCode(code);
    }

    @Override
    public List<Device> findByOwnerId(UUID ownerId) {
        return jpaRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Device> findByStatus(DeviceStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public Device save(Device device) {
        return jpaRepository.saveAndFlush(device);
    }
}
