package com.rabbitmqsample.notificationservice.service;

import com.rabbitmqsample.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqsample.notificationservice.model.event.OrderCreatedEvent;

import java.util.List;

public interface NotificationService {
    void createNotification(OrderCreatedEvent event);
    NotificationResponseDto getById(String id);
    List<NotificationResponseDto> getAll();
    List<NotificationResponseDto> getByOrderId(String orderId);
    List<NotificationResponseDto> getByCustomerEmail(String customerEmail);
}