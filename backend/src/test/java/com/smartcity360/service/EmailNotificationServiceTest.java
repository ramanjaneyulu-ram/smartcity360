package com.smartcity360.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailNotificationServiceTest {

    @Test
    void sendsEmailThroughResendApi() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("https://api.resend.com/emails"))
        .andExpect(method(POST))
        .andExpect(header("Authorization", "Bearer test-api-key"))
        .andExpect(content().json("""
            {"from":"alerts@example.com","to":["admin@example.com"],"subject":"Complaint Resolved","text":"A complaint was resolved."}
            """))
        .andRespond(withSuccess("{}", APPLICATION_JSON));
    EmailNotificationService service = new EmailNotificationService(
        builder, "test-api-key", "alerts@example.com");

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

    server.verify();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    void doesNotSendEmailWhenRecipientAddressIsMissing(String recipientAddress) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EmailNotificationService service = new EmailNotificationService(
            builder, "test-api-key", "alerts@example.com");

        service.sendEmailNotification(recipientAddress, "Complaint Resolved", "A complaint was resolved.");

        server.verify();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void doesNotSendEmailWhenFromAddressIsMissing(String fromAddress) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EmailNotificationService service = new EmailNotificationService(
            builder, "test-api-key", fromAddress);

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

        server.verify();
    }

    @Test
    void doesNotSendEmailWhenResendApiKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EmailNotificationService service = new EmailNotificationService(
                builder, "", "alerts@example.com");

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

        server.verify();
    }

    @Test
        void doesNotPropagateResendApiFailures() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
            .andRespond(withServerError());
        EmailNotificationService service = new EmailNotificationService(
            builder, "test-api-key", "alerts@example.com");

        assertDoesNotThrow(() ->
                service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved."));

        server.verify();
    }
}