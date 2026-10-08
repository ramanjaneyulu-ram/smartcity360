package com.smartcity360.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClassifyRequest {
    @NotBlank
    private String description;
}
