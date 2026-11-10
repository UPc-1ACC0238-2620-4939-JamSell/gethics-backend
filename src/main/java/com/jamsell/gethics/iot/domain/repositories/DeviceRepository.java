package com.jamsell.gethics.iot.domain.repositories;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository {

    /** {@code code} ya normalizado con {@link Device#normalizeCode}. */
    boolean existsByCode(String code);

    /** {@code code} ya normalizado con {@link Device#normalizeCode}. */
    Optional<Device> findByCode(String code);

    List<Device> findByOwnerId(UUID ownerId);

    List<Device> findByStatus(DeviceStatus status);

    boolean existsById(UUID deviceId);

    /** US-37: lecturas del dispositivo de la mas reciente a la mas antigua ({@code recordedAt} descendente). */
    SensorReadingPage findReadingsByDeviceId(UUID deviceId, int page, int size);

    /** Persiste el dispositivo y devuelve la instancia persistida (con id asignado). */
    Device save(Device device);
}
