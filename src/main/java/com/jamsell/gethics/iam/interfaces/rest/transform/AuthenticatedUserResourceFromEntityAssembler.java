package com.jamsell.gethics.iam.interfaces.rest.transform;

import com.jamsell.gethics.iam.domain.services.UserCommandService.AuthenticatedUser;
import com.jamsell.gethics.iam.interfaces.rest.resources.AuthenticatedUserResource;

public class AuthenticatedUserResourceFromEntityAssembler {

    public static AuthenticatedUserResource toResourceFromEntity(AuthenticatedUser authenticatedUser) {
        return new AuthenticatedUserResource(
                authenticatedUser.accessToken(),
                authenticatedUser.refreshToken(),
                "Bearer",
                authenticatedUser.expiresInSeconds(),
                UserResourceFromEntityAssembler.toResourceFromEntity(authenticatedUser.user())
        );
    }
}
