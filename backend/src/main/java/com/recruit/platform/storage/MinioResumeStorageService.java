package com.recruit.platform.storage;

import com.recruit.platform.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "minio")
public class MinioResumeStorageService implements ResumeStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @PostConstruct
    void ensureBucket() {
        try {
            if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder()
                    .bucket(storageProperties.minio().bucket())
                    .build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(storageProperties.minio().bucket()).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize MinIO bucket", exception);
        }
    }

    @Override
    public String store(String objectKey, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.minio().bucket())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to upload file to MinIO", exception);
        }
    }

    @Override
    public Resource loadAsResource(String objectKey) {
        return new InputStreamResource(openStream(objectKey));
    }

    @Override
    public InputStream openStream(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder().bucket(storageProperties.minio().bucket()).object(objectKey).build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load file from MinIO", exception);
        }
    }

    @Configuration
    static class MinioConfig {

        @Bean
        @ConditionalOnProperty(name = "app.storage.mode", havingValue = "minio")
        MinioClient minioClient(StorageProperties storageProperties) {
            return MinioClient.builder()
                    .endpoint(storageProperties.minio().endpoint())
                    .credentials(storageProperties.minio().accessKey(), storageProperties.minio().secretKey())
                    .build();
        }
    }
}
