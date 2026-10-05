package com.jamsell.gethics.iot.interfaces.scheduling;

import com.jamsell.gethics.iot.domain.model.commands.SynchronizeDevicesCommand;
import com.jamsell.gethics.iot.domain.services.DeviceCommandService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Disparador periodico (adapter de entrada): solo delega en el servicio de aplicacion. La periodicidad no esta
 * definida por el negocio: sin {@code gethics.iot.sync.cron} queda deshabilitado ({@link Scheduled#CRON_DISABLED}).
 */
@Component
public class DeviceSyncJob {

    static final String CRON = "${gethics.iot.sync.cron:" + Scheduled.CRON_DISABLED + "}";

    private final DeviceCommandService commandService;

    public DeviceSyncJob(DeviceCommandService commandService) {
        this.commandService = commandService;
    }

    @Scheduled(cron = CRON)
    public void run() {
        commandService.handle(new SynchronizeDevicesCommand());
    }
}
