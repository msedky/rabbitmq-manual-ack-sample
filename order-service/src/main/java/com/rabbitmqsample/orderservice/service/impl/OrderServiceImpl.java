package com.rabbitmqsample.orderservice.service.impl;

import com.rabbitmqsample.orderservice.exception.NotFoundException;
import com.rabbitmqsample.orderservice.mapper.OrderDetailMapper;
import com.rabbitmqsample.orderservice.mapper.OrderMapper;
import com.rabbitmqsample.orderservice.model.dto.request.CreateOrderRequestDto;
import com.rabbitmqsample.orderservice.model.dto.response.OrderResponseDto;
import com.rabbitmqsample.orderservice.model.entity.OrderDetailEntity;
import com.rabbitmqsample.orderservice.model.entity.OrderEntity;
import com.rabbitmqsample.orderservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.orderservice.repository.OrderRepository;
import com.rabbitmqsample.orderservice.service.OrderEventPublisher;
import com.rabbitmqsample.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderEventPublisher orderEventPublisher;

    @Override
    @Transactional
    public OrderResponseDto createOrder(CreateOrderRequestDto requestDto) {
        OrderEntity orderEntity = orderMapper.toEntity(requestDto);

        Instant now = Instant.now();
        orderEntity.setCreatedAt(now);
        orderEntity.setLastUpdatedAt(now);

        Set<OrderDetailEntity> detailEntities = requestDto.getOrderDetails()
                .stream()
                .map(orderDetailMapper::toEntity)
                .peek(detail -> {
                    detail.setOrder(orderEntity);
                    detail.setCreatedAt(now);
                    detail.setLastUpdatedAt(now);

                    float total = detail.getProductPrice() * detail.getQuantity();
                    detail.setTotalPrice(total);
                })
                .collect(Collectors.toSet());

        orderEntity.setOrderDetails(detailEntities);

        float orderTotalPrice = (float) detailEntities.stream()
                .mapToDouble(OrderDetailEntity::getTotalPrice)
                .sum();

        orderEntity.setTotalPrice(orderTotalPrice);

        log.info("saving order [customerId=" + orderEntity.getCustomerId() + ",customerName=" + orderEntity.getCustomerName() + "] .....");

        OrderEntity savedOrder = orderRepository.save(orderEntity);


        orderEventPublisher.publishOrderCreatedEvent(
                new OrderCreatedEvent(savedOrder.getId(), savedOrder.getCustomerEmail())
        );

        return orderMapper.toResponseDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(UUID orderId) {
        log.info("getting order by id : " + orderId + " .....");
        OrderEntity orderEntity = orderRepository.findByIdWithOrderDetails(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

        return orderMapper.toResponseDto(orderEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        log.info("getting all orders  .....");
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponseDto)
                .toList();
    }
}