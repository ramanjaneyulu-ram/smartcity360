package com.smartcity360.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailHost;
    private final String fromAddress;

    public EmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${app.mail.from:${spring.mail.username:}}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendEmailNotification(String recipientAddress, String subject, String body) {
        if (recipientAddress == null || recipientAddress.isBlank()) {
            return;
        }

        if (mailHost == null || mailHost.isBlank()) {
            log.debug("SMTP is not configured; skipping email notification to {}", recipientAddress);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP is configured but no mail sender is available; skipping email to {}",
                    recipientAddress);
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipientAddress);
        email.setSubject(subject);
        email.setText(body);
        if (fromAddress != null && !fromAddress.isBlank()) {
            email.setFrom(fromAddress);
        }

        try {
            mailSender.send(email);
            log.info("Email notification sent to {}", recipientAddress);
        } catch (MailException exception) {
            log.error("Failed to send email notification to {}", recipientAddress, exception);
        }
    }
}