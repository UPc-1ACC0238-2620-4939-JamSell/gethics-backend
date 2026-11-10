package com.jamsell.gethics.livestock.interfaces.acl;

import java.util.UUID;

/** Vista minima de un animal para otros bounded contexts: ninguna entidad de Livestock sale de este contexto. */
public record AnimalSummary(UUID id, String name, String tag, String breed, String status) {
}
