package com.jamsell.gethics.iot.domain.model.commands;

import java.util.UUID;

/**
 * US-23 Escenario 1: el ganadero vincula un dispositivo IoT compatible desde la app usando su codigo.
 */
public record LinkDeviceCommand(UUID ownerId, String code) {
}
