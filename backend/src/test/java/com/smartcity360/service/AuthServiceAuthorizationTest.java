package com.smartcity360.service;

import com.smartcity360.dto.RegisterRequest;
import com.smartcity360.model.Role;
import com.smartcity360.model.User;
import com.smartcity360.repository.UserRepository;
import com.smartcity360.security.JwtService;
import com.smartcity360.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceAuthorizationTest {

    @Test
    void publicRegistrationRejectsAdminRole() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(
                userRepository, passwordEncoder, jwtService, mock(AuthenticationManager.class));
        RegisterRequest request = new RegisterRequest();
        request.setName("Unauthorized Admin");
        request.setEmail("admin@example.com");
        request.setPassword("password123");
        request.setRole(Role.ADMIN);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verifyNoInteractions(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void publicRegistrationCreatesCitizenWhenRoleIsOmitted() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(
                userRepository, passwordEncoder, jwtService, mock(AuthenticationManager.class));
        RegisterRequest request = new RegisterRequest();
        request.setName("New Citizen");
        request.setEmail("citizen@example.com");
        request.setPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(UserDetailsImpl.class), anyMap())).thenReturn("token");

        assertEquals("CITIZEN", authService.register(request).getRole());
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user -> user.getRole() == Role.CITIZEN));
        verify(userRepository, never()).findByEmail("citizen@example.com");
    }
}