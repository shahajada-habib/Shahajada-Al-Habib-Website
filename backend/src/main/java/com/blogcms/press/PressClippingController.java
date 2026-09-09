package com.blogcms.press;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/press")
public class PressClippingController {

    private final PressClippingService service;

    public PressClippingController(PressClippingService service) {
        this.service = service;
    }

    @GetMapping
    public List<PressClipping> list() {
        return service.listAll();
    }

    /** Pre-fills the admin form from the linked page's Open Graph tags. */
    @PostMapping("/preview")
    public LinkPreview preview(@RequestBody Map<String, String> body) {
        return service.preview(body.get("url"));
    }

    @PostMapping
    public PressClipping create(@RequestBody PressClippingDto request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PressClipping update(@PathVariable Long id, @RequestBody PressClippingDto request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
