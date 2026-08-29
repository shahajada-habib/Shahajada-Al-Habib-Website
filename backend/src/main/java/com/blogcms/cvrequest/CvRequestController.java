package com.blogcms.cvrequest;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cv-requests")
public class CvRequestController {

    private final CvRequestService cvRequestService;

    public CvRequestController(CvRequestService cvRequestService) {
        this.cvRequestService = cvRequestService;
    }

    @GetMapping
    public List<CvRequestResponseDto> list() {
        return cvRequestService.list();
    }

    @PatchMapping("/{id}/status")
    public CvRequestResponseDto updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return cvRequestService.updateStatus(id, body.get("status"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cvRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> file() {
        Resource resource = cvRequestService.cvFile();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cv.pdf\"")
                .body(resource);
    }
}
