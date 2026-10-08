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
public class ComplaintHistoryResponse {
    private String status;
    private String message;
    private String actorName;
    private LocalDateTime createdAt;
}