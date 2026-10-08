package com.smartcity360.dto;

import com.smartcity360.model.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull
    private ComplaintStatus status;

    /** Optional — used when status is RESOLVED. */
    private String resolutionNote;
    private String evidenceUrl;
}
