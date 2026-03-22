package com.rabbitmqsample.notificationservice.model.dto.response;

import com.rabbitmqsample.notificationservice.model.enums.NotificationStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class NotificationResponseDto {
    private String id;
    private String orderId;
    private String customerEmail;
    private String type;
    private String message;
    private NotificationStatus status;
    private Instant createdAt;
}