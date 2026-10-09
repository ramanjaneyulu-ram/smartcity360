package com.smartcity360.service;

import com.smartcity360.dto.NotificationResponse;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.Notification;
import com.smartcity360.model.NotificationType;
import com.smartcity360.model.User;
import com.smartcity360.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public Notification createNotification(User recipient, String title, String message,
                                          Complaint complaint, NotificationType type) {
        if (recipient == null) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .complaint(complaint)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        emailNotificationService.sendEmailNotification(recipient.getEmail(), title, message);

        return saved;
    }

    public List<NotificationResponse> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public List<NotificationResponse> getNotificationsForComplaint(Long complaintId) {
        return notificationRepository.findByComplaintIdOrderByCreatedAtDesc(complaintId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public Map<String, Long> getUnreadCount(User user) {
        long count = notificationRepository.countByUserAndReadFalse(user);
        return Map.of("unreadCount", count);
    }

    @Transactional
    public void markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found."));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

}
