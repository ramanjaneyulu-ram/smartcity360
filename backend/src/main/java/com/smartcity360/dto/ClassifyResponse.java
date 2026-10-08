package com.smartcity360.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ClassifyResponse {
    private String category;
    private String department;
    private String priority;
    private int confidence; // 0-100
}
