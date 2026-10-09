package com.smartcity360.service;

import com.smartcity360.model.Role;
import com.smartcity360.model.User;
import com.smartcity360.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final boolean resetPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.reset-password:false}") boolean resetPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.resetPassword = resetPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Both bootstrap admin email and password must be configured.");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Bootstrap admin password must be at least 12 characters.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User existingUser = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() != Role.ADMIN) {
                throw new IllegalStateException("Bootstrap admin email is already used by a non-admin account.");
            }
            if (resetPassword) {
                existingUser.setPassword(passwordEncoder.encode(password));
                userRepository.save(existingUser);
            }
            return;
        }

        User admin = User.builder()
                .name("SmartCity 360 Admin")
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
    }
}