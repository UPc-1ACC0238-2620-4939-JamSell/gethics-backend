package com.jamsell.gethics.iot.domain.services;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import java.util.List;

public interface DeviceQueryService {

    List<Device> handle(GetDevicesByOwnerQuery query);
}
