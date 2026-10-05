package com.jamsell.gethics.livestock.domain.model.valueobjects;

/** Estado del registro del animal. Un animal nuevo siempre nace ACTIVE; las bajas (US-08) son logicas. */
public enum AnimalStatus {
    ACTIVE,
    SOLD,
    DECEASED,
    INACTIVE
}
