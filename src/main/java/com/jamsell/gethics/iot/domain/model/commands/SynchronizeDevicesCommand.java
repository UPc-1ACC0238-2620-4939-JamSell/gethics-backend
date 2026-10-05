package com.jamsell.gethics.iot.domain.model.commands;

/**
 * US-23 Escenario 2: intento periodico de sincronizacion. Los dispositivos CONNECTED sin lecturas recientes pasan a
 * DISCONNECTED y se notifica al ganadero.
 */
public record SynchronizeDevicesCommand() {
}
