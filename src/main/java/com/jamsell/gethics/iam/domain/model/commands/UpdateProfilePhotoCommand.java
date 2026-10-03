package com.jamsell.gethics.iam.domain.model.commands;

import com.jamsell.gethics.iam.domain.model.valueobjects.ProfileImage;

public record UpdateProfilePhotoCommand(Long userId, ProfileImage image) {

    public UpdateProfilePhotoCommand {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (image == null) {
            throw new IllegalArgumentException("La imagen es obligatoria");
        }
    }
}
