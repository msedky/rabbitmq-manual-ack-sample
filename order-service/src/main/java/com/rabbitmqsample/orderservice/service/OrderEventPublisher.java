package com.rabbitmqsample.orderservice.service;

import com.rabbitmqsample.orderservice.model.event.OrderCreatedEvent;

public interface OrderEventPublisher {
    void publishOrderCreatedEvent(OrderCreatedEvent event);
}