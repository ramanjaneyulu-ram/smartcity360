package com.smartcity360.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintCreateRequest {
    @NotBlank
    private String description;

    private String location;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
}
