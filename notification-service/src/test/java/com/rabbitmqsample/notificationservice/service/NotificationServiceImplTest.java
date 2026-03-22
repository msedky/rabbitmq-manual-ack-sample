package com.rabbitmqsample.notificationservice.service;

import com.rabbitmqsample.notificationservice.exception.NotFoundException;
import com.rabbitmqsample.notificationservice.mapper.NotificationMapperImpl;
import com.rabbitmqsample.notificationservice.model.document.NotificationDocument;
import com.rabbitmqsample.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqsample.notificationservice.model.enums.NotificationStatus;
import com.rabbitmqsample.notificationservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.notificationservice.repository.NotificationRepository;
import com.rabbitmqsample.notificationservice.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        NotificationServiceImpl.class,
        NotificationMapperImpl.class
})
class NotificationServiceImplTest {

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Test
    void createNotification_success() {
        UUID orderId = UUID.randomUUID();
        String email = "msedky@example.com";

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(orderId);
        event.setCustomerEmail(email);

        NotificationDocument doc = NotificationDocument
                .builder()
                .orderId(UUID.randomUUID().toString())
                .customerEmail(email)
                .type("EMAIL")
                .message("Order created successfully for customer " + email)
                .status(NotificationStatus.RECEIVED)
                .createdAt(LocalDateTime.now().minusDays(56).toInstant(ZoneOffset.UTC))
                .build();

        NotificationDocument savedDocument = NotificationDocument
                .builder()
                .id(UUID.randomUUID().toString())
                .orderId(doc.getOrderId())
                .customerEmail(doc.getCustomerEmail())
                .type(doc.getType())
                .message(doc.getMessage())
                .status(doc.getStatus())
                .createdAt(doc.getCreatedAt())
                .build();

        when(notificationRepository.save(doc))
                .thenReturn(savedDocument);

        notificationService.createNotification(event);

        verify(notificationRepository, times(1)).save(argThat(document ->
                document.getOrderId().equals(orderId.toString()) &&
                        document.getCustomerEmail().equals(email) &&
                        document.getType().equals(savedDocument.getType()) &&
                        document.getMessage().equals(savedDocument.getMessage()) &&
                        document.getStatus().equals(savedDocument.getStatus()) &&
                        document.getStatus().equals(NotificationStatus.RECEIVED) &&
                        document.getCreatedAt() != null
        ));
    }

    @Test
    void getAll_success() {
        NotificationDocument doc1 = NotificationDocument.builder()
                .id(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerEmail("user1@example.com")
                .type("EMAIL")
                .message("msg-1")
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        NotificationDocument doc2 = NotificationDocument.builder()
                .id(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerEmail("user2@example.com")
                .type("EMAIL")
                .message("msg-2")
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findAll()).thenReturn(List.of(doc1, doc2));

        List<NotificationResponseDto> result = notificationService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(doc1.getId(), result.get(0).getId());
        assertEquals(doc1.getCustomerEmail(), result.get(0).getCustomerEmail());

        assertEquals(doc2.getId(), result.get(1).getId());
        assertEquals(doc2.getCustomerEmail(), result.get(1).getCustomerEmail());

        verify(notificationRepository, times(1)).findAll();
    }

    @Test
    void getAll_emptyList() {
        when(notificationRepository.findAll()).thenReturn(List.of());

        List<NotificationResponseDto> result = notificationService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(notificationRepository, times(1)).findAll();
    }


    @Test
    void findNotification_by_id_exists() {

        UUID id = UUID.randomUUID();
        String email = "msedky@example.com";

        NotificationDocument savedDocument = NotificationDocument
                .builder()
                .id(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerEmail(email)
                .type("EMAIL")
                .message("Order created successfully for customer " + email)
                .status(NotificationStatus.RECEIVED)
                .createdAt(LocalDateTime.now().minusDays(56).toInstant(ZoneOffset.UTC))
                .build();

        when(notificationRepository.findById(id.toString())).thenReturn(Optional.of(savedDocument));

        NotificationResponseDto notificationResponseDto = notificationService.getById(id.toString());

        assertEquals(savedDocument.getId(), notificationResponseDto.getId());
    }

    @Test
    void findNotification_by_id_not_exists() {
        UUID id = UUID.randomUUID();

        when(notificationRepository.findById(id.toString())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> notificationService.getById(id.toString())
        );

        assertEquals("Notification not found with id: " + id.toString(), exception.getMessage());
    }

    @Test
    void getByOrderId_success() {
        String orderId = UUID.randomUUID().toString();

        NotificationDocument doc1 = NotificationDocument.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerEmail("user1@example.com")
                .type("EMAIL")
                .message("msg-1")
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        NotificationDocument doc2 = NotificationDocument.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerEmail("user2@example.com")
                .type("EMAIL")
                .message("msg-2")
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByOrderId(orderId)).thenReturn(List.of(doc1, doc2));

        List<NotificationResponseDto> result = notificationService.getByOrderId(orderId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> orderId.equals(n.getOrderId())));

        verify(notificationRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getByOrderId_emptyList() {
        String orderId = UUID.randomUUID().toString();

        when(notificationRepository.findByOrderId(orderId)).thenReturn(List.of());

        List<NotificationResponseDto> result = notificationService.getByOrderId(orderId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(notificationRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getByCustomerEmail_success() {
        String email = "msedky@example.com";

        NotificationDocument doc1 = NotificationDocument.builder()
                .id(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerEmail(email)
                .type("EMAIL")
                .message("msg-1")
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        NotificationDocument doc2 = NotificationDocument.builder()
                .id(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerEmail(email)
                .type("EMAIL")
                .message("msg-2")
                .status(NotificationStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByCustomerEmail(email)).thenReturn(List.of(doc1, doc2));

        List<NotificationResponseDto> result = notificationService.getByCustomerEmail(email);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> email.equals(n.getCustomerEmail())));

        verify(notificationRepository, times(1)).findByCustomerEmail(email);
    }

    @Test
    void getByCustomerEmail_emptyList() {
        String email = "notfound@example.com";

        when(notificationRepository.findByCustomerEmail(email)).thenReturn(List.of());

        List<NotificationResponseDto> result = notificationService.getByCustomerEmail(email);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(notificationRepository, times(1)).findByCustomerEmail(email);
    }
}