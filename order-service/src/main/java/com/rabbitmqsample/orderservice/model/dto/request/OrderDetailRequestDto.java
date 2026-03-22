package com.rabbitmqsample.orderservice.model.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class OrderDetailRequestDto {
    private UUID productId;
    private String productName;
    private Float productPrice;
    private Integer quantity;
}