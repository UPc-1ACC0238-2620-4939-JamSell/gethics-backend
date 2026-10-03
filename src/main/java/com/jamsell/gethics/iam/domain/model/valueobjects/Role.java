package com.jamsell.gethics.iam.domain.model.valueobjects;

public enum Role {
    GANADERO,
    VETERINARIO;

    public static Role fromName(String name) {
        if (name != null) {
            for (var role : values()) {
                if (role.name().equalsIgnoreCase(name.trim())) {
                    return role;
                }
            }
        }
        throw new IllegalArgumentException("El rol debe ser GANADERO o VETERINARIO");
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
