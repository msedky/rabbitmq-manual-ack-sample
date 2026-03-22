package com.rabbitmqsample.orderservice.service.impl;

import com.rabbitmqsample.orderservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.orderservice.service.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisherImpl implements OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "order.exchange";
    private static final String ROUTING_KEY = "order.created";

    @Override
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("publishing event " + event + " ...");
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}