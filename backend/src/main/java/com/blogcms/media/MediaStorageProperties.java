package com.blogcms.media;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaStorageProperties {

    private final Path uploadRoot;

    public MediaStorageProperties(@Value("${app.media.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path uploadRoot() {
        return uploadRoot;
    }
}
