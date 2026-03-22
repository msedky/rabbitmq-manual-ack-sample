package com.rabbitmqsample.orderservice.model.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
public class OrderResponseDto {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private Float totalPrice;
    private Set<OrderDetailResponseDto> orderDetails;
    private Instant createdAt;
    private Instant lastUpdatedAt;
}