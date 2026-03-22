package com.rabbitmqsample.orderservice.service;

import com.rabbitmqsample.orderservice.model.dto.request.CreateOrderRequestDto;
import com.rabbitmqsample.orderservice.model.dto.response.OrderResponseDto;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponseDto createOrder(CreateOrderRequestDto requestDto);

    OrderResponseDto getOrderById(UUID orderId);

    List<OrderResponseDto> getAllOrders();
}