package com.jamsell.gethics.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResource user
) {
}
