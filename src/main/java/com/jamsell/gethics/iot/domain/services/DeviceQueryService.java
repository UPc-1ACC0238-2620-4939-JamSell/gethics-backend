package com.jamsell.gethics.iot.domain.services;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.queries.GetDeviceReadingsQuery;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
import java.util.List;

public interface DeviceQueryService {

    List<Device> handle(GetDevicesByOwnerQuery query);

    /** US-37. Lanza {@code DeviceNotFoundException} si el dispositivo no existe. */
    SensorReadingPage handle(GetDeviceReadingsQuery query);
}
