package com.blogcms.auth;

import com.blogcms.security.JwtService;
import com.blogcms.user.User;
import com.blogcms.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        String username = request.username() == null ? "" : request.username().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Login failed for unknown username={}", username);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password");
                });

        if (!"active".equals(user.getStatus()) || !passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed for username={}, active={}", username, "active".equals(user.getStatus()));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }

        log.info("Login succeeded for username={}, role={}", user.getUsername(), user.getRole());
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getRole());
    }
}
