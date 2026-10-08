package com.smartcity360.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt-hashed

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Only meaningful for OFFICER accounts, e.g. "Public Works", "Electrical". */
    private String department;

    /** Only meaningful for OFFICER accounts, e.g. "Ward 7". */
    private String ward;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
