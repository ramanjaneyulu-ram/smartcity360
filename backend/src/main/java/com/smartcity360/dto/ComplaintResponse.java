package com.smartcity360.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {
    private Long id;
    private String publicId;      // e.g. "SC-20394"
    private String description;
    private String category;
    private String department;
    private String priority;
    private String status;
    private String location;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String citizenName;
    private String officerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime slaDueAt;
    private boolean overdue;
}
