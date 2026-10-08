package com.smartcity360.service;

import com.smartcity360.dto.NotificationResponse;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.Notification;
import com.smartcity360.model.NotificationType;
import com.smartcity360.model.User;
import com.smartcity360.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

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

        sendEmailNotification(recipient, title, message);

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

    public void sendEmailNotification(User recipient, String subject, String body) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            return;
        }

        if (mailHost == null || mailHost.isBlank()) {
            log.debug("SMTP is not configured; skipping email notification to {}", recipient.getEmail());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP is configured but no mail sender is available; skipping email to {}",
                    recipient.getEmail());
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient.getEmail());
        email.setSubject(subject);
        email.setText(body);
        if (fromAddress != null && !fromAddress.isBlank()) {
            email.setFrom(fromAddress);
        }

        try {
            mailSender.send(email);
            log.info("Email notification sent to {}", recipient.getEmail());
        } catch (MailException exception) {
            log.error("Failed to send email notification to {}", recipient.getEmail(), exception);
        }
    }
}
