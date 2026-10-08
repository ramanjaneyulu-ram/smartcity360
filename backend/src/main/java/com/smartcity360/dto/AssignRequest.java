package com.smartcity360.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignRequest {
    @NotNull
    private Long officerId;
}
