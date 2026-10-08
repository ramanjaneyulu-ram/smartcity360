package com.smartcity360.controller;

import com.smartcity360.dto.NotificationResponse;
import com.smartcity360.security.UserDetailsImpl;
import com.smartcity360.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal UserDetailsImpl principal) {
        return notificationService.getNotificationsForUser(principal.getUser());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal UserDetailsImpl principal) {
        return notificationService.getUnreadCount(principal.getUser());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetailsImpl principal) {
        notificationService.markAsRead(id, principal.getUser());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal UserDetailsImpl principal) {
        notificationService.markAllAsRead(principal.getUser());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/complaint/{complaintId}")
    public List<NotificationResponse> getComplaintNotifications(@PathVariable Long complaintId) {
        return notificationService.getNotificationsForComplaint(complaintId);
    }
}
