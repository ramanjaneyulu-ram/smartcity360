package com.smartcity360.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${app.email.resend.api-key:}") String apiKey,
            @Value("${app.email.from:}") String fromAddress,
            @Value("${app.frontend.url:https://smartcity360-oqd7.vercel.app}") String frontendUrl) {
        this.restClient = restClientBuilder.baseUrl("https://api.resend.com").build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Async
    public void sendEmailNotification(String recipientAddress, String subject, String body) {
        if (recipientAddress == null || recipientAddress.isBlank()) {
            return;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Resend API key is not configured; skipping email notification to {}", recipientAddress);
            return;
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Resend sender address is not configured; skipping email notification to {}", recipientAddress);
            return;
        }

        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", fromAddress,
                            "to", List.of(recipientAddress),
                            "subject", subject,
                                "text", body + "\n\nSign in to SmartCity 360 to view details:\n"
                                    + frontendUrl + "/login"))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email notification sent to {}", recipientAddress);
        } catch (RestClientException exception) {
            log.error("Failed to send email notification to {} through Resend", recipientAddress, exception);
        }
    }
}