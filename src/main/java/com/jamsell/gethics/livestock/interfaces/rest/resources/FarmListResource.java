package com.jamsell.gethics.livestock.interfaces.rest.resources;

import java.util.List;

/** {@code message} solo trae valor cuando {@code farms} esta vacio ("No hay granjas registradas."). */
public record FarmListResource(List<FarmResource> farms, String message) {
}
