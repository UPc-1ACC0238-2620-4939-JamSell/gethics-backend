package com.jamsell.gethics.livestock.interfaces.rest.resources;

import java.util.List;

/** {@code message} solo trae valor cuando {@code animals} esta vacio ("Sin resultados." o "No hay animales registrados."). */
public record AnimalListResource(List<AnimalResource> animals, String message) {
}
