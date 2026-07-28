package com.blogcms.user;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blogcms.common.ContentSanitizer;
import com.blogcms.common.InputValidator;
import com.blogcms.security.CurrentUserService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("admin", "editor", "reporter");
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "inactive");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final InputValidator inputValidator;
    private final ContentSanitizer sanitizer;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService,
            InputValidator inputValidator,
            ContentSanitizer sanitizer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.inputValidator = inputValidator;
        this.sanitizer = sanitizer;
    }

    public List<UserResponseDto> getUsers() {
        currentUserService.requireAdmin("list users");
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponseDto createUser(UserRequestDto request) {
        currentUserService.requireAdmin("create users");

        String username = inputValidator.username(request.getUsername());
        String password = inputValidator.password(request.getPassword());
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(normalizeRole(request.getRole()));
        user.setStatus(normalizeStatus(request.getStatus()));
        applyProfileFields(user, request);
        return toResponse(userRepository.save(user));
    }

    public UserResponseDto updateRole(Long id, Map<String, String> request) {
        currentUserService.requireAdmin("update user role");
        User user = getUser(id);
        user.setRole(normalizeRole(request.get("role")));
        return toResponse(userRepository.save(user));
    }

    public UserResponseDto updateStatus(Long id, Map<String, String> request) {
        currentUserService.requireAdmin("update user status");
        User user = getUser(id);
        user.setStatus(normalizeStatus(request.get("status")));
        return toResponse(userRepository.save(user));
    }

    public UserResponseDto updateProfile(Long id, UserRequestDto request) {
        currentUserService.requireAdmin("update user profile");
        User user = getUser(id);
        applyProfileFields(user, request);
        return toResponse(userRepository.save(user));
    }

    public UserResponseDto updateMyProfile(UserRequestDto request) {
        User user = userRepository.findByUsername(currentUserService.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
        applyProfileFields(user, request);
        return toResponse(userRepository.save(user));
    }

    public void changeMyPassword(String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(currentUserService.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        if (!passwordEncoder.matches(currentPassword == null ? "" : currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "current password is incorrect");
        }

        String validatedNewPassword = inputValidator.password(newPassword);
        user.setPassword(passwordEncoder.encode(validatedNewPassword));
        userRepository.save(user);
    }

    public UserResponseDto getUserProfile(String username) {
        return userRepository.findByUsernameAndPublicProfileTrue(username.toLowerCase())
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "journalist not found"));
    }

    public List<UserResponseDto> getAllPublicJournalists() {
        return userRepository.findPublicJournalists().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<WriterOptionDto> getActiveWriters() {
        currentUserService.requireEditorOrAdmin("list reporters");
        return userRepository.findActiveWriters().stream()
                .map((user) -> new WriterOptionDto(user.getId(), user.getUsername(), displayName(user)))
                .toList();
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    private UserResponseDto toResponse(User user) {
        UserResponseDto response = new UserResponseDto();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setDesignation(user.getDesignation());
        response.setBio(user.getBio());
        response.setProfileImageUrl(user.getProfileImageUrl());
        response.setFacebookUrl(user.getFacebookUrl());
        response.setTwitterUrl(user.getTwitterUrl());
        response.setEmailPublic(user.getEmailPublic());
        response.setPublic(user.isPublicProfile());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    private void applyProfileFields(User user, UserRequestDto request) {
        user.setDisplayName(optionalPlainText(request.getDisplayName(), 150));
        user.setDesignation(optionalPlainText(request.getDesignation(), 100));
        user.setBio(optionalPlainText(request.getBio(), 500));
        user.setProfileImageUrl(inputValidator.optional(request.getProfileImageUrl(), 500));
        user.setFacebookUrl(inputValidator.optional(request.getFacebookUrl(), 300));
        user.setTwitterUrl(inputValidator.optional(request.getTwitterUrl(), 300));
        user.setEmailPublic(inputValidator.optional(request.getEmailPublic(), 200));
        if (request.getIsPublic() != null) {
            user.setPublicProfile(request.getIsPublic());
        }
    }

    private String optionalPlainText(String value, int maxLength) {
        return inputValidator.optional(sanitizer.plainText(value), maxLength);
    }

    private String displayName(User user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
    }

    private String normalizeRole(String value) {
        String role = normalizeRequired(value, "role is required");
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role must be admin, editor, or reporter");
        }
        return role;
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "active";
        }

        String status = value.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be active or inactive");
        }
        return status;
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value exceeds 64 characters");
        }
        return normalized;
    }
}
