package com.jamsell.gethics.iot.domain.services;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.commands.RegisterSensorReadingCommand;
import com.jamsell.gethics.iot.domain.model.commands.SynchronizeDevicesCommand;
import com.jamsell.gethics.iot.domain.model.entities.SensorReading;

public interface DeviceCommandService {

    Device handle(LinkDeviceCommand command);

    SensorReading handle(RegisterSensorReadingCommand command);

    void handle(SynchronizeDevicesCommand command);
}
