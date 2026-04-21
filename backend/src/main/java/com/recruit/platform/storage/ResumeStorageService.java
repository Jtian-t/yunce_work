package com.recruit.platform.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeStorageService {

    String store(String objectKey, MultipartFile file);

    Resource loadAsResource(String objectKey);

    InputStream openStream(String objectKey);
}
