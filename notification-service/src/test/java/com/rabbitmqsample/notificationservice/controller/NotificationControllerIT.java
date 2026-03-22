package com.rabbitmqsample.notificationservice.controller;

import com.rabbitmqsample.notificationservice.model.document.NotificationDocument;
import com.rabbitmqsample.notificationservice.model.enums.NotificationStatus;
import com.rabbitmqsample.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class NotificationControllerIT {

    @Container
    @ServiceConnection
    static final MongoDBContainer mongoContainer =
            new MongoDBContainer("mongo:7.0.12");


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void getAll_shouldReturnAllNotifications() throws Exception {
        NotificationDocument doc1 = buildNotification(
                UUID.randomUUID().toString(),
                "user1@test.com",
                "msg-1"
        );

        NotificationDocument doc2 = buildNotification(
                UUID.randomUUID().toString(),
                "user2@test.com",
                "msg-2"
        );

        notificationRepository.saveAll(List.of(doc1, doc2));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAll_shouldReturnEmptyList_whenNoNotificationsExist() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getById_shouldReturnNotification_whenExists() throws Exception {
        NotificationDocument saved = notificationRepository.save(
                buildNotification(UUID.randomUUID().toString(), "user@test.com", "msg-1")
        );

        mockMvc.perform(get("/api/v1/notifications/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.orderId").value(saved.getOrderId()))
                .andExpect(jsonPath("$.customerEmail").value(saved.getCustomerEmail()))
                .andExpect(jsonPath("$.type").value(saved.getType()))
                .andExpect(jsonPath("$.message").value(saved.getMessage()))
                .andExpect(jsonPath("$.status").value(saved.getStatus().name()));
    }

    @Test
    void getById_shouldReturnNotFound_whenDoesNotExist() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/notifications/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByOrderId_shouldReturnMatchingNotifications() throws Exception {
        String targetOrderId = UUID.randomUUID().toString();

        NotificationDocument doc1 = buildNotification(targetOrderId, "user1@test.com", "msg-1");
        NotificationDocument doc2 = buildNotification(targetOrderId, "user2@test.com", "msg-2");
        NotificationDocument other = buildNotification(UUID.randomUUID().toString(), "other@test.com", "msg-3");

        notificationRepository.saveAll(List.of(doc1, doc2, other));

        mockMvc.perform(get("/api/v1/notifications/order/{orderId}", targetOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId").value(targetOrderId))
                .andExpect(jsonPath("$[1].orderId").value(targetOrderId));
    }

    @Test
    void getByOrderId_shouldReturnEmptyList_whenNoMatchesFound() throws Exception {
        String orderId = UUID.randomUUID().toString();

        notificationRepository.save(
                buildNotification(UUID.randomUUID().toString(), "user@test.com", "msg-1")
        );

        mockMvc.perform(get("/api/v1/notifications/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getByCustomerEmail_shouldReturnMatchingNotifications() throws Exception {
        String email = "msedky@example.com";

        NotificationDocument doc1 = buildNotification(UUID.randomUUID().toString(), email, "msg-1");
        NotificationDocument doc2 = buildNotification(UUID.randomUUID().toString(), email, "msg-2");
        NotificationDocument other = buildNotification(UUID.randomUUID().toString(), "other@example.com", "msg-3");

        notificationRepository.saveAll(List.of(doc1, doc2, other));

        mockMvc.perform(get("/api/v1/notifications/customer")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerEmail").value(email))
                .andExpect(jsonPath("$[1].customerEmail").value(email));
    }

    @Test
    void getByCustomerEmail_shouldReturnEmptyList_whenNoMatchesFound() throws Exception {
        notificationRepository.save(
                buildNotification(UUID.randomUUID().toString(), "other@example.com", "msg-1")
        );

        mockMvc.perform(get("/api/v1/notifications/customer")
                        .param("email", "notfound@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    private NotificationDocument buildNotification(String orderId, String email, String message) {
        return NotificationDocument.builder()
                .orderId(orderId)
                .customerEmail(email)
                .type("EMAIL")
                .message(message)
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();
    }
}