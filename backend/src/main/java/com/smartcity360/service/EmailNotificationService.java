package com.smartcity360.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final int MAX_SEND_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationService(
            JavaMailSender mailSender,
            @Value("${app.email.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendEmailNotification(String recipientAddress, String subject, String body) {
        if (recipientAddress == null || recipientAddress.isBlank()) {
            return;
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Mail sender is not configured; skipping email notification to {}", recipientAddress);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientAddress);
        message.setSubject(subject);
        message.setText(body);

        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {
            try {
                mailSender.send(message);
                log.info("Email notification sent to {}", recipientAddress);
                return;
            } catch (MailException exception) {
                if (attempt == MAX_SEND_ATTEMPTS) {
                    log.error("Email notification to {} failed after {} attempts",
                            recipientAddress, MAX_SEND_ATTEMPTS, exception);
                    return;
                }

                log.warn("Email notification to {} failed on attempt {}; retrying",
                        recipientAddress, attempt, exception);
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry interrupted for email notification to {}", recipientAddress);
                    return;
                }
            }
        }
    }
}