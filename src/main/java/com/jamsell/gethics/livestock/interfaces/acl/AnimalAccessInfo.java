package com.jamsell.gethics.livestock.interfaces.acl;

import java.util.UUID;

/** Estado y dueno de un animal para otros bounded contexts. {@code ownerId} es null si el animal no tiene granja. */
public record AnimalAccessInfo(UUID animalId, boolean active, UUID ownerId) {
}
