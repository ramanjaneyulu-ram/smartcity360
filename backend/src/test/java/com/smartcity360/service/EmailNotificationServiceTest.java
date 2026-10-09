package com.smartcity360.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationServiceTest {

    @Test
    void sendsEmailToRecipientAddress() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        EmailNotificationService service = new EmailNotificationService(
                provider,
                "smtp.example.com",
                "sender@example.com");

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

        var emailCaptor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(emailCaptor.capture());
        assertArrayEquals(new String[]{"admin@example.com"}, emailCaptor.getValue().getTo());
        assertEquals("Complaint Resolved", emailCaptor.getValue().getSubject());
        assertEquals("A complaint was resolved.", emailCaptor.getValue().getText());
        assertEquals("sender@example.com", emailCaptor.getValue().getFrom());
    }
}