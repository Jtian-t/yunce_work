package com.recruit.platform.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AppAuthProperties(
        String issuer,
        Duration accessTokenExpiry,
        Duration refreshTokenExpiry,
        String secret
) {
}
