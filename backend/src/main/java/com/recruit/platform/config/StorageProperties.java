package com.recruit.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String mode,
        String localBasePath,
        Minio minio
) {

    public record Minio(
            String endpoint,
            String accessKey,
            String secretKey,
            String bucket
    ) {
    }
}
