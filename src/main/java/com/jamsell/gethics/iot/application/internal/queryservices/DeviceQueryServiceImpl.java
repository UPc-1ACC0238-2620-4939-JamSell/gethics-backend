package com.jamsell.gethics.iot.application.internal.queryservices;

import com.jamsell.gethics.iot.domain.exceptions.DeviceNotFoundException;
import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.queries.GetDeviceReadingsQuery;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import com.jamsell.gethics.iot.domain.services.DeviceQueryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

    private final DeviceRepository repository;

    public DeviceQueryServiceImpl(DeviceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> handle(GetDevicesByOwnerQuery query) {
        return repository.findByOwnerId(query.ownerId());
    }

    /** US-37: un dispositivo sin lecturas devuelve una pagina vacia (Escenario 2), no un error. */
    @Override
    @Transactional(readOnly = true)
    public SensorReadingPage handle(GetDeviceReadingsQuery query) {
        if (!repository.existsById(query.deviceId())) {
            throw new DeviceNotFoundException(query.deviceId());
        }
        return repository.findReadingsByDeviceId(query.deviceId(), query.page(), query.size());
    }
}
