package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

    // US-37: la lectura mas reciente primero; el id desempata lecturas con el mismo recordedAt para que el orden
    // entre paginas sea estable.
    private static final Sort NEWEST_READINGS_FIRST = Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id"));

    private final DeviceJpaRepository jpaRepository;
    private final SensorReadingJpaRepository readingJpaRepository;

    public DeviceRepositoryImpl(DeviceJpaRepository jpaRepository, SensorReadingJpaRepository readingJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.readingJpaRepository = readingJpaRepository;
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
    public boolean existsById(UUID deviceId) {
        return jpaRepository.existsById(deviceId);
    }

    @Override
    public SensorReadingPage findReadingsByDeviceId(UUID deviceId, int page, int size) {
        var result = readingJpaRepository.findByDeviceId(deviceId, PageRequest.of(page, size, NEWEST_READINGS_FIRST));
        return new SensorReadingPage(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Device save(Device device) {
        return jpaRepository.saveAndFlush(device);
    }
}
