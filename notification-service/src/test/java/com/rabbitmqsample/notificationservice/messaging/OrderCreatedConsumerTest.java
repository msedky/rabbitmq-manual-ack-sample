package com.rabbitmqsample.notificationservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqsample.notificationservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringJUnitConfig
@ContextConfiguration(classes = OrderCreatedConsumer.class)
class OrderCreatedConsumerTest {

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private OrderCreatedConsumer orderCreatedConsumer;

    @Test
    void handle_shouldDelegateToNotificationService_andAckMessage_whenProcessingSucceeds() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(UUID.randomUUID());
        event.setCustomerEmail("msedky@example.com");

        Channel channel = mock(Channel.class);
        long deliveryTag = 1L;

        orderCreatedConsumer.handle(event, channel, deliveryTag);

        verify(notificationService, times(1)).createNotification(event);
        verify(channel, times(1)).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(deliveryTag, false, true);
    }

    @Test
    void handle_shouldNackMessage_whenProcessingFails() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(UUID.randomUUID());
        event.setCustomerEmail("msedky@example.com");

        Channel channel = mock(Channel.class);
        long deliveryTag = 1L;

        doThrow(new RuntimeException("Simulated failure"))
                .when(notificationService).createNotification(event);

        orderCreatedConsumer.handle(event, channel, deliveryTag);

        verify(notificationService, times(1)).createNotification(event);
        verify(channel, never()).basicAck(deliveryTag, false);
        verify(channel, times(1)).basicNack(deliveryTag, false, true);
    }

    @Test
    void handle_shouldLogError_whenProcessingFails_andNackAlsoFails() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(UUID.randomUUID());
        event.setCustomerEmail("msedky@example.com");

        Channel channel = mock(Channel.class);
        long deliveryTag = 1L;

        doThrow(new RuntimeException("Processing failure"))
                .when(notificationService).createNotification(event);

        doThrow(new IOException("Nack failure"))
                .when(channel).basicNack(deliveryTag, false, true);

        orderCreatedConsumer.handle(event, channel, deliveryTag);

        verify(notificationService, times(1)).createNotification(event);
        verify(channel, never()).basicAck(deliveryTag, false);
        verify(channel, times(1)).basicNack(deliveryTag, false, true);
    }
}