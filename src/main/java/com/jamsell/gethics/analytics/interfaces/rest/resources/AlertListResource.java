package com.jamsell.gethics.analytics.interfaces.rest.resources;

import java.util.List;

/** {@code message} solo trae valor cuando {@code alerts} esta vacio ("No hay alertas registradas."). */
public record AlertListResource(List<AlertResource> alerts, String message) {
}
