package com.jamsell.gethics.iot.interfaces.rest.transform;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.commands.RegisterSensorReadingCommand;
import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import com.jamsell.gethics.iot.interfaces.rest.resources.DeviceResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.LinkDeviceResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.RegisterSensorReadingResource;
import com.jamsell.gethics.iot.interfaces.rest.resources.SensorReadingResource;
import java.util.List;

public final class DeviceAssembler {

    private DeviceAssembler() {
    }

    public static LinkDeviceCommand toCommand(LinkDeviceResource resource) {
        return new LinkDeviceCommand(resource.ownerId(), resource.code());
    }

    public static RegisterSensorReadingCommand toCommand(String code, RegisterSensorReadingResource resource) {
        return new RegisterSensorReadingCommand(code, resource.metric(), resource.value(), resource.recordedAt());
    }

    public static DeviceResource toResource(Device device) {
        return new DeviceResource(device.getId(), device.getOwnerId(), device.getCode(), device.getStatus(),
                device.getLinkedAt(), device.getLastReadingAt());
    }

    public static List<DeviceResource> toResourceList(List<Device> devices) {
        return devices.stream().map(DeviceAssembler::toResource).toList();
    }

    public static SensorReadingResource toResource(SensorReading reading) {
        return new SensorReadingResource(reading.getId(), reading.getMetric(), reading.getValue(),
                reading.getRecordedAt());
    }
}
