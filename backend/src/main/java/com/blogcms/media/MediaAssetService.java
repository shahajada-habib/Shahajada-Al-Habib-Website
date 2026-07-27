package com.blogcms.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.blogcms.common.ContentSanitizer;
import com.blogcms.security.CurrentUserService;

import jakarta.annotation.PostConstruct;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class MediaAssetService {

    private static final Logger log = LoggerFactory.getLogger(MediaAssetService.class);
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final MediaAssetRepository mediaAssetRepository;
    private final CurrentUserService currentUserService;
    private final ContentSanitizer sanitizer;
    private final Path uploadRoot;
    private final long maxFileSizeBytes;

    public MediaAssetService(
            MediaAssetRepository mediaAssetRepository,
            CurrentUserService currentUserService,
            ContentSanitizer sanitizer,
            MediaStorageProperties mediaStorageProperties,
            @Value("${app.media.max-file-size-bytes:5242880}") long maxFileSizeBytes) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.currentUserService = currentUserService;
        this.sanitizer = sanitizer;
        this.uploadRoot = mediaStorageProperties.uploadRoot();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostConstruct
    void createUploadDirectory() throws IOException {
        Files.createDirectories(uploadRoot);
        log.info("Media upload directory ready at {}", uploadRoot);
    }

    public List<MediaAsset> getAll() {
        currentUserService.requireEditorOrAdmin("view media");
        return mediaAssetRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<MediaAsset> getGallery() {
        return mediaAssetRepository.findByShowInGalleryTrueOrderByCreatedAtDesc();
    }

    public MediaAsset updateGallery(Long id, boolean showInGallery, String caption) {
        currentUserService.requireEditorOrAdmin("update gallery visibility");
        MediaAsset mediaAsset = mediaAssetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "media not found"));
        mediaAsset.setShowInGallery(showInGallery);
        mediaAsset.setCaption(normalizeDisplayName(caption, null));
        return mediaAssetRepository.save(mediaAsset);
    }

    public MediaAsset upload(MultipartFile file, String title) {
        String username = currentUserService.username();
        validateUpload(file);

        String originalFileName = safeFileName(file.getOriginalFilename());
        String displayName = normalizeDisplayName(title, originalFileName);
        String storedFileName = UUID.randomUUID() + "-" + originalFileName;
        Path destination = uploadRoot.resolve(storedFileName).normalize();

        if (!destination.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid file path");
        }

        log.info(
                "Media upload request: user={}, fileName={}, contentType={}, size={}, destination={}",
                username,
                originalFileName,
                file.getContentType(),
                file.getSize(),
                destination);

        try {
            file.transferTo(destination);
        } catch (IOException exception) {
            log.warn("Media upload failed at {}", destination, exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "could not store file");
        }

        MediaAsset mediaAsset = new MediaAsset();
        mediaAsset.setFileName(displayName);
        mediaAsset.setFileUrl(fileUrl(storedFileName));
        mediaAsset.setContentType(normalizeContentType(file.getContentType()));
        mediaAsset.setSize(file.getSize());
        mediaAsset.setUploadedBy(username);
        MediaAsset savedAsset = mediaAssetRepository.save(mediaAsset);
        log.info("Media upload saved: id={}, fileUrl={}", savedAsset.getId(), savedAsset.getFileUrl());
        return savedAsset;
    }

    public boolean delete(Long id) {
        currentUserService.requireEditorOrAdmin("delete media");
        Optional<MediaAsset> mediaAsset = mediaAssetRepository.findById(id);

        if (mediaAsset.isEmpty()) {
            return false;
        }

        deleteStoredFile(mediaAsset.get());
        mediaAssetRepository.deleteById(id);
        return true;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Media upload rejected: empty file");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }

        if (file.getSize() > maxFileSizeBytes) {
            log.warn("Media upload rejected: file too large size={} max={}", file.getSize(), maxFileSizeBytes);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file exceeds maximum upload size");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.containsKey(contentType)) {
            log.warn("Media upload rejected: unsupported contentType={}", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only jpg, png, webp, or gif images are allowed");
        }

        String extension = extensionOf(safeFileName(file.getOriginalFilename()));
        String expectedExtension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (!extension.equals(expectedExtension) && !("image/jpeg".equals(contentType) && ".jpeg".equals(extension))) {
            log.warn("Media upload rejected: extension/content-type mismatch extension={} contentType={}", extension, contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file extension does not match image type");
        }
    }

    private String normalizeDisplayName(String title, String fallback) {
        if (title == null || title.isBlank()) {
            return fallback;
        }

        String normalized = sanitizer.plainText(title);
        if (normalized.isBlank()) {
            return fallback;
        }
        if (normalized.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "media title exceeds 255 characters");
        }
        return normalized;
    }

    private String safeFileName(String fileName) {
        String fallback = "image";
        String cleanName = fileName == null || fileName.isBlank() ? fallback : Path.of(fileName).getFileName().toString();
        cleanName = cleanName.replaceAll("[^A-Za-z0-9._-]", "-");
        return cleanName.isBlank() ? fallback : cleanName;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? "" : fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String fileUrl(String storedFileName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(storedFileName)
                .toUriString();
    }

    private void deleteStoredFile(MediaAsset mediaAsset) {
        String fileUrl = mediaAsset.getFileUrl();
        String storedFileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path target = uploadRoot.resolve(storedFileName).normalize();

        if (!target.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "could not delete file");
        }
    }
}
