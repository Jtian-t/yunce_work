package com.recruit.platform.security;

import com.recruit.platform.config.AppAuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final AppAuthProperties authProperties;

    public String generateAccessToken(PlatformUserPrincipal principal) {
        return generateToken(
                principal.getUsername(),
                Map.of(
                        "uid", principal.getUserId(),
                        "displayName", principal.getDisplayName(),
                        "roles", principal.getRoles()
                ),
                authProperties.accessTokenExpiry().toSeconds()
        );
    }

    public String generateRefreshToken(PlatformUserPrincipal principal) {
        return generateToken(principal.getUsername(), Map.of("uid", principal.getUserId(), "type", "refresh"),
                authProperties.refreshTokenExpiry().toSeconds());
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isExpired(String token) {
        return parse(token).getExpiration().before(new Date());
    }

    private String generateToken(String subject, Map<String, ?> claims, long expirySeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuer(authProperties.issuer())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(now.plusSeconds(expirySeconds).toInstant()))
                .signWith(signingKey())
                .compact();
    }

    private Key signingKey() {
        byte[] keyBytes = authProperties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
