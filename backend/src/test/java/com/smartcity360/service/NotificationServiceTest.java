package com.smartcity360.service;

import com.smartcity360.dto.NotificationResponse;
import com.smartcity360.model.Complaint;
import com.smartcity360.model.Notification;
import com.smartcity360.model.NotificationType;
import com.smartcity360.model.Role;
import com.smartcity360.model.User;
import com.smartcity360.repository.NotificationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    private JavaMailSender mailSender;
    private NotificationService notificationService;
    private User testUser;
    private Complaint testComplaint;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        mailSenderProvider = mock(ObjectProvider.class);
        mailSender = mock(JavaMailSender.class);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        notificationService = new NotificationService(notificationRepository, mailSenderProvider);
        ReflectionTestUtils.setField(notificationService, "mailHost", "smtp.example.com");

        testUser = User.builder()
                .id(1L)
                .name("Test Citizen")
                .email("citizen@example.com")
                .role(Role.CITIZEN)
                .build();

        testComplaint = Complaint.builder()
                .id(5L)
                .description("Pothole on Main St")
                .category("Road Damage")
                .department("Public Works")
                .build();
    }

    @Test
    void createNotificationSavesAndSendsEmailToLoginAddress() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(101L);
            return n;
        });

        Notification result = notificationService.createNotification(
                testUser,
                "Report Submitted",
                "Your report has been received.",
                testComplaint,
                NotificationType.COMPLAINT_SUBMITTED
        );

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("Report Submitted", result.getTitle());
        assertFalse(result.isRead());
        verify(notificationRepository, times(1)).save(any(Notification.class));

        var emailCaptor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        assertArrayEquals(new String[]{"citizen@example.com"}, emailCaptor.getValue().getTo());
        assertEquals("Report Submitted", emailCaptor.getValue().getSubject());
        assertEquals("Your report has been received.", emailCaptor.getValue().getText());
    }

    @Test
    void getUnreadCountReturnsCorrectCount() {
        when(notificationRepository.countByUserAndReadFalse(testUser)).thenReturn(3L);

        Map<String, Long> countMap = notificationService.getUnreadCount(testUser);

        assertEquals(3L, countMap.get("unreadCount"));
    }

    @Test
    void markAsReadUpdatesStatus() {
        Notification notification = Notification.builder()
                .id(10L)
                .user(testUser)
                .read(false)
                .title("Test")
                .message("Test message")
                .type(NotificationType.STATUS_UPDATED)
                .build();

        when(notificationRepository.findByIdAndUser(10L, testUser)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(10L, testUser);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAllAsReadDelegatesToRepository() {
        notificationService.markAllAsRead(testUser);
        verify(notificationRepository).markAllAsReadForUser(testUser);
    }
}
