package com.blogcms.auth;

import com.blogcms.user.User;
import com.blogcms.user.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DefaultUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedEnabled;
    private final String defaultPassword;
    private final boolean resetPassword;

    public DefaultUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.default-users.enabled:false}") boolean seedEnabled,
            @Value("${app.seed.default-password:1234}") String defaultPassword,
            @Value("${app.seed.default-users.reset-password:false}") boolean resetPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
        this.defaultPassword = defaultPassword;
        this.resetPassword = resetPassword;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        createOrResetUser("admin", "admin");
        createOrResetUser("editor", "editor");
        createOrResetUser("reporter", "reporter");
    }

    private void createOrResetUser(String username, String role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                user -> {
                    if (!resetPassword) {
                        return;
                    }

                    user.setPassword(passwordEncoder.encode(defaultPassword));
                    user.setRole(role);
                    user.setStatus("active");
                    userRepository.save(user);
                },
                () -> createUser(username, role));
    }

    private void createUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setRole(role);
        user.setStatus("active");
        userRepository.save(user);
    }
}
