package com.recruit.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent")
public record AppAgentProperties(
        String callbackBaseUrl,
        int timeoutDays,
        boolean enabled,
        boolean fallbackToMock,
        String serviceBaseUrl,
        String parsePath,
        String analyzePath,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
