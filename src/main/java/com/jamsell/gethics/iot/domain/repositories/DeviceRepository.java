package com.jamsell.gethics.iot.domain.repositories;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
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

    /** Persiste el dispositivo y devuelve la instancia persistida (con id asignado). */
    Device save(Device device);
}
