package com.jamsell.gethics.iam.application.internal.outboundservices.tokens;

import com.jamsell.gethics.iam.domain.model.aggregates.User;

import java.util.Optional;

public interface TokenService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    long getAccessTokenExpirationSeconds();

    Optional<String> getEmailFromAccessToken(String token);
}
