package com.rabbitmqsample.orderservice.model.dto.request;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class CreateOrderRequestDto {
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private Set<OrderDetailRequestDto> orderDetails;
}