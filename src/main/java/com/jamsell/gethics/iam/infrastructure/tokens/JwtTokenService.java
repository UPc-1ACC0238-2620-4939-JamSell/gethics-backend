package com.jamsell.gethics.iam.infrastructure.tokens;

import com.jamsell.gethics.iam.application.internal.outboundservices.tokens.TokenService;
import com.jamsell.gethics.iam.domain.model.aggregates.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtTokenService implements TokenService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpirationMinutes;
    private final long refreshExpirationDays;

    public JwtTokenService(
            @Value("${authorization.jwt.secret}") String secret,
            @Value("${authorization.jwt.access-expiration-minutes}") long accessExpirationMinutes,
            @Value("${authorization.jwt.refresh-expiration-days}") long refreshExpirationDays
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMinutes = accessExpirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    @Override
    public String generateAccessToken(User user) {
        return buildToken(user, ACCESS, Duration.ofMinutes(accessExpirationMinutes));
    }

    @Override
    public String generateRefreshToken(User user) {
        return buildToken(user, REFRESH, Duration.ofDays(refreshExpirationDays));
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return Duration.ofMinutes(accessExpirationMinutes).toSeconds();
    }

    @Override
    public Optional<String> getEmailFromAccessToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!ACCESS.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String buildToken(User user, String type, Duration validity) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(signingKey)
                .compact();
    }
}
