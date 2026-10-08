package com.smartcity360.service;

import com.smartcity360.dto.*;
import com.smartcity360.model.Role;
import com.smartcity360.model.Role;
import com.smartcity360.model.User;
import com.smartcity360.repository.UserRepository;
import com.smartcity360.security.JwtService;
import com.smartcity360.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest req) {
        if (req.getRole() != null && req.getRole() != Role.CITIZEN) {
            throw new IllegalArgumentException("Public registration is only available for citizens.");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.CITIZEN)
                .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public OfficerResponse createOfficer(OfficerCreateRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User officer = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.OFFICER)
                .department(req.getDepartment())
                .ward(req.getWard())
                .build();
        officer = userRepository.save(officer);
        return OfficerResponse.from(officer);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());
        claims.put("userId", user.getId());

        String token = jwtService.generateToken(new UserDetailsImpl(user), claims);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .build();
    }
}
