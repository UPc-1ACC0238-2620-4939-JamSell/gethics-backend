package com.jamsell.gethics.iot.application.internal.queryservices;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
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
}
