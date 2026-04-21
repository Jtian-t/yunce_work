package com.recruit.platform.auth;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.Set;

record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}

record AuthResponse(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        Set<String> roles,
        String displayName
) {
}

record MeResponse(
        Long id,
        String username,
        String displayName,
        String email,
        String department,
        Set<String> roles
) {
}
