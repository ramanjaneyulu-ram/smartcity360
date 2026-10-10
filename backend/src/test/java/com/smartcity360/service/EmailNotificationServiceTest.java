package com.smartcity360.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class EmailNotificationServiceTest {

    @Test
    void sendsEmailThroughConfiguredSmtpSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationService service = new EmailNotificationService(mailSender, "alerts@gmail.com");

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

        org.mockito.ArgumentCaptor<SimpleMailMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("alerts@gmail.com", message.getFrom());
        assertEquals("admin@example.com", message.getTo()[0]);
        assertEquals("Complaint Resolved", message.getSubject());
        assertEquals("A complaint was resolved.", message.getText());
        verifyNoMoreInteractions(mailSender);
    }

    @Test
    void retriesAfterTransientSmtpFailure() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("Temporary SMTP failure"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailNotificationService service = new EmailNotificationService(mailSender, "alerts@gmail.com");

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

        verify(mailSender, org.mockito.Mockito.times(2)).send(any(SimpleMailMessage.class));
        verifyNoMoreInteractions(mailSender);
    }
}