package com.blogcms.media;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MediaAssetController {

    private final MediaAssetService mediaAssetService;

    public MediaAssetController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    @PostMapping(value = "/api/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaAsset upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title) {
        return mediaAssetService.upload(file, title);
    }

    @GetMapping("/api/media")
    public List<MediaAsset> getAll() {
        return mediaAssetService.getAll();
    }

    @DeleteMapping("/api/media/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return mediaAssetService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/api/media/{id}/gallery")
    public MediaAsset updateGallery(@PathVariable Long id, @RequestBody GalleryUpdateRequest request) {
        return mediaAssetService.updateGallery(id, request.showInGallery(), request.caption());
    }

    @GetMapping("/api/public/media/gallery")
    public List<MediaAsset> getPublicGallery() {
        return mediaAssetService.getGallery();
    }

    public record GalleryUpdateRequest(boolean showInGallery, String caption) {
    }
}
