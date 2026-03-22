package com.rabbitmqsample.notificationservice.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmqsample.notificationservice.config.RabbitConfig;
import com.rabbitmqsample.notificationservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handle(OrderCreatedEvent event,
                       Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("consuming published event " + event + " .....");
        try {
            notificationService.createNotification(event);
            channel.basicAck(deliveryTag, false);
            log.info("Message acknowledged successfully for orderId={}", event.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to process message for orderId={}", event.getOrderId(), ex);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception nackEx) {
                log.error("Failed to nack message for orderId={}", event.getOrderId(), nackEx);
            }
        }
    }
}