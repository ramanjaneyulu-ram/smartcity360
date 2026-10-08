package com.smartcity360.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resolutions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false, unique = true)
    private Complaint complaint;

    @Column(length = 1000)
    private String description;

    private String evidenceUrl;

    @Builder.Default
    private LocalDateTime resolvedAt = LocalDateTime.now();
}
