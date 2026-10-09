package com.smartcity360.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    private static final String RESEND_EMAILS_ENDPOINT = "https://api.resend.com/emails";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;

    public EmailNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${app.email.resend.api-key:}") String apiKey,
            @Value("${app.email.from:}") String fromAddress) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendEmailNotification(String recipientAddress, String subject, String body) {
        if (recipientAddress == null || recipientAddress.isBlank()) {
            return;
        }

        if (apiKey == null || apiKey.isBlank() || fromAddress == null || fromAddress.isBlank()) {
            log.warn("Resend is not configured; skipping email notification to {}", recipientAddress);
            return;
        }

        try {
            restClient.post()
                    .uri(RESEND_EMAILS_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", fromAddress,
                            "to", List.of(recipientAddress),
                            "subject", subject,
                            "text", body))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email notification sent to {}", recipientAddress);
        } catch (RestClientException exception) {
            log.error("Failed to send email notification to {}", recipientAddress, exception);
        }
    }
}