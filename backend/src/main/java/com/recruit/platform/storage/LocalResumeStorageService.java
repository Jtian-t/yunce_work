package com.recruit.platform.storage;

import com.recruit.platform.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalResumeStorageService implements ResumeStorageService {

    private final StorageProperties storageProperties;
    private Path basePath;

    @PostConstruct
    void init() throws IOException {
        this.basePath = Path.of(storageProperties.localBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
    }

    @Override
    public String store(String objectKey, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Path target = basePath.resolve(objectKey).normalize();
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return objectKey;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store file", exception);
        }
    }

    @Override
    public Resource loadAsResource(String objectKey) {
        return new FileSystemResource(basePath.resolve(objectKey).normalize());
    }

    @Override
    public InputStream openStream(String objectKey) {
        try {
            return Files.newInputStream(basePath.resolve(objectKey).normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to open resume", exception);
        }
    }
}
