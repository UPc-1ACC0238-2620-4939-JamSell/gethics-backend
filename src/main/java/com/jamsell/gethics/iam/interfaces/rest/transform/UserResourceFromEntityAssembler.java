package com.jamsell.gethics.iam.interfaces.rest.transform;

import com.jamsell.gethics.iam.domain.model.aggregates.User;
import com.jamsell.gethics.iam.interfaces.rest.resources.UserResource;

public class UserResourceFromEntityAssembler {

    public static final String PHOTO_URL = "/users/me/photo";

    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPhone(),
                user.hasPhoto() ? PHOTO_URL : null
        );
    }
}
