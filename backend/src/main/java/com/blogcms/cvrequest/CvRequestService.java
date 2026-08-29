package com.blogcms.cvrequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.blogcms.common.ContentSanitizer;
import com.blogcms.common.InputValidator;
import com.blogcms.security.CurrentUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CvRequestService {

    private static final Logger log = LoggerFactory.getLogger(CvRequestService.class);
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "sent", "declined");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String CV_CLASSPATH_LOCATION = "private/cv.pdf";

    private final CvRequestRepository cvRequestRepository;
    private final CurrentUserService currentUserService;
    private final ContentSanitizer sanitizer;
    private final InputValidator inputValidator;

    public CvRequestService(
            CvRequestRepository cvRequestRepository,
            CurrentUserService currentUserService,
            ContentSanitizer sanitizer,
            InputValidator inputValidator) {
        this.cvRequestRepository = cvRequestRepository;
        this.currentUserService = currentUserService;
        this.sanitizer = sanitizer;
        this.inputValidator = inputValidator;
    }

    /**
     * Records a public request for the CV. {@code honeypot} must stay empty — a
     * value there means a bot filled a hidden field, so the submission is dropped
     * without persisting (and without telling the client, to avoid tipping off
     * scripted spam).
     */
    public void create(String name, String email, String purpose, String honeypot) {
        if (honeypot != null && !honeypot.isBlank()) {
            log.info("CV_REQUEST_SPAM_DROPPED honeypot triggered");
            return;
        }

        String cleanName = inputValidator.required(sanitizer.plainText(name), "name", 120);
        String cleanEmail = inputValidator.required(sanitizer.plainText(email), "email", 200);
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is invalid");
        }
        String cleanPurpose = inputValidator.required(sanitizer.plainText(purpose), "purpose", 2000);

        CvRequest request = new CvRequest();
        request.setName(cleanName);
        request.setEmail(cleanEmail);
        request.setPurpose(cleanPurpose);
        request.setStatus("pending");
        CvRequest saved = cvRequestRepository.save(request);
        log.info("NEW_CV_REQUEST id={} email={}", saved.getId(), saved.getEmail());
    }

    public List<CvRequestResponseDto> list() {
        currentUserService.requireEditorOrAdmin("list CV requests");
        return cvRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(CvRequestResponseDto::from)
                .toList();
    }

    public CvRequestResponseDto updateStatus(Long id, String status) {
        currentUserService.requireEditorOrAdmin("update CV requests");
        String normalized = status == null ? "" : status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be one of " + ALLOWED_STATUSES);
        }
        CvRequest request = getRequest(id);
        request.setStatus(normalized);
        request.setHandledAt("pending".equals(normalized) ? null : LocalDateTime.now());
        return CvRequestResponseDto.from(cvRequestRepository.save(request));
    }

    public void delete(Long id) {
        currentUserService.requireEditorOrAdmin("delete CV requests");
        if (!cvRequestRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV request not found");
        }
        cvRequestRepository.deleteById(id);
    }

    public long pendingCount() {
        return cvRequestRepository.countByStatus("pending");
    }

    /** The private CV file, for the owner to download and attach when replying. */
    public Resource cvFile() {
        currentUserService.requireEditorOrAdmin("download the CV file");
        Resource resource = new ClassPathResource(CV_CLASSPATH_LOCATION);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV file is not present");
        }
        return resource;
    }

    private CvRequest getRequest(Long id) {
        return cvRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CV request not found"));
    }
}
