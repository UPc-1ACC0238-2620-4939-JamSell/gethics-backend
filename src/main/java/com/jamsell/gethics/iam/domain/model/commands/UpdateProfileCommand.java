package com.jamsell.gethics.iam.domain.model.commands;

import com.jamsell.gethics.iam.domain.model.valueobjects.PhoneNumber;

public record UpdateProfileCommand(Long userId, String name, String phone) {

    public UpdateProfileCommand {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        name = name.trim();
        phone = PhoneNumber.normalizeOrNull(phone);
    }
}
