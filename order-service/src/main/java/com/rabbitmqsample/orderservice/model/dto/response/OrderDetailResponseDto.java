package com.rabbitmqsample.orderservice.model.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OrderDetailResponseDto {
    private UUID id;
    private UUID productId;
    private String productName;
    private Float productPrice;
    private Integer quantity;
    private Float totalPrice;
    private Instant createdAt;
    private Instant lastUpdatedAt;
}