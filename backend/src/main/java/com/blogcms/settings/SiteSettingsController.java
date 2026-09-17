package com.blogcms.settings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
public class SiteSettingsController {

    private final SiteSettingsService service;

    public SiteSettingsController(SiteSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public SiteSettingsDto get() {
        return SiteSettingsDto.from(service.get());
    }

    @PutMapping
    public SiteSettingsDto update(@RequestBody SiteSettingsDto request) {
        return SiteSettingsDto.from(service.update(request));
    }
}
