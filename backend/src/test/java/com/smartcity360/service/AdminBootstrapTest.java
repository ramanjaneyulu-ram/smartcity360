package com.smartcity360.service;

import com.smartcity360.model.Role;
import com.smartcity360.model.User;
import com.smartcity360.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    @Test
    void resetSwitchChangesExistingAdminPasswordUsingEncoder() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User admin = User.builder()
                .email("admin@example.com")
                .password("old-hash")
                .role(Role.ADMIN)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("A-new-private-password-123")).thenReturn("new-hash");

        AdminBootstrap bootstrap = new AdminBootstrap(
                userRepository,
                passwordEncoder,
                "admin@example.com",
                "A-new-private-password-123",
                true);
        bootstrap.run(null);

        assertEquals("new-hash", admin.getPassword());
        verify(userRepository).save(admin);
    }

    @Test
    void resetSwitchDoesNotChangeNonAdminAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User citizen = User.builder()
                .email("citizen@example.com")
                .password("old-hash")
                .role(Role.CITIZEN)
                .build();
        when(userRepository.findByEmail("citizen@example.com")).thenReturn(Optional.of(citizen));

        AdminBootstrap bootstrap = new AdminBootstrap(
                userRepository,
                passwordEncoder,
                "citizen@example.com",
                "A-new-private-password-123",
                true);

        assertThrows(IllegalStateException.class, () -> bootstrap.run(null));
        assertEquals("old-hash", citizen.getPassword());
        verify(userRepository, never()).save(citizen);
    }
}