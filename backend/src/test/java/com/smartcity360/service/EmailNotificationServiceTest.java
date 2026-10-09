package com.smartcity360.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmailNotificationServiceTest {

    @Test
    void sendsEmailThroughResendHttpsApi() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EmailNotificationService service = new EmailNotificationService(
        builder,
        "test-api-key",
        "SmartCity 360 <onboarding@resend.dev>");

    server.expect(requestTo("https://api.resend.com/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer test-api-key"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "from": "SmartCity 360 <onboarding@resend.dev>",
              "to": ["admin@example.com"],
              "subject": "Complaint Resolved",
              "text": "A complaint was resolved."
            }
            """))
        .andRespond(withSuccess("{\"id\":\"email-id\"}", MediaType.APPLICATION_JSON));

        service.sendEmailNotification("admin@example.com", "Complaint Resolved", "A complaint was resolved.");

    server.verify();
    }
}