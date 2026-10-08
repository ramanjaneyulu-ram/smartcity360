package com.smartcity360.dto;

import com.smartcity360.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private Long complaintId;
    private String complaintPublicId;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        Long complaintId = n.getComplaint() != null ? n.getComplaint().getId() : null;
        String publicId = complaintId != null ? "SC-" + (20000 + complaintId) : null;

        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : null)
                .read(n.isRead())
                .complaintId(complaintId)
                .complaintPublicId(publicId)
                .createdAt(n.getCreatedAt())
                .build();
    }
}
