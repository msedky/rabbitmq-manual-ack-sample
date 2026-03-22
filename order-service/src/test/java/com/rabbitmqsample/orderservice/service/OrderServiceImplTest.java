package com.rabbitmqsample.orderservice.service;

import com.rabbitmqsample.orderservice.exception.NotFoundException;
import com.rabbitmqsample.orderservice.mapper.OrderDetailMapperImpl;
import com.rabbitmqsample.orderservice.mapper.OrderMapperImpl;
import com.rabbitmqsample.orderservice.model.dto.request.CreateOrderRequestDto;
import com.rabbitmqsample.orderservice.model.dto.request.OrderDetailRequestDto;
import com.rabbitmqsample.orderservice.model.dto.response.OrderResponseDto;
import com.rabbitmqsample.orderservice.model.entity.OrderDetailEntity;
import com.rabbitmqsample.orderservice.model.entity.OrderEntity;
import com.rabbitmqsample.orderservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.orderservice.repository.OrderRepository;
import com.rabbitmqsample.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        OrderServiceImpl.class,
        OrderMapperImpl.class,
        OrderDetailMapperImpl.class
})
class OrderServiceImplTest {

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private OrderService orderService;

    @Test
    void createOrder_shouldSaveOrder_publishEvent_andReturnResponse() {
        UUID customerId = UUID.randomUUID();
        UUID product1Id = UUID.randomUUID();
        UUID product2Id = UUID.randomUUID();
        UUID savedOrderId = UUID.randomUUID();

        OrderDetailRequestDto detail1 = new OrderDetailRequestDto();
        detail1.setProductId(product1Id);
        detail1.setProductName("Laptop");
        detail1.setProductPrice(1000f);
        detail1.setQuantity(2);

        OrderDetailRequestDto detail2 = new OrderDetailRequestDto();
        detail2.setProductId(product2Id);
        detail2.setProductName("Mouse");
        detail2.setProductPrice(50f);
        detail2.setQuantity(3);

        CreateOrderRequestDto request = new CreateOrderRequestDto();
        request.setCustomerId(customerId);
        request.setCustomerName("Mohammad Sedky");
        request.setCustomerEmail("mohammad@example.com");
        request.setOrderDetails(Set.of(detail1, detail2));

        Instant now = Instant.now();

        OrderEntity entity = OrderEntity.builder()
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .createdAt(now)
                .lastUpdatedAt(now)
                .build();

        entity.setOrderDetails(Set.of(
                OrderDetailEntity.builder()
                        .order(entity)
                        .productId(detail1.getProductId())
                        .productName(detail1.getProductName())
                        .productPrice(detail1.getProductPrice())
                        .quantity(detail1.getQuantity())
                        .totalPrice(detail1.getQuantity() * detail1.getProductPrice())
                        .createdAt(now)
                        .lastUpdatedAt(now)
                        .build(),
                OrderDetailEntity.builder()
                        .order(entity)
                        .productId(detail2.getProductId())
                        .productName(detail2.getProductName())
                        .productPrice(detail2.getProductPrice())
                        .quantity(detail2.getQuantity())
                        .totalPrice(detail2.getQuantity() * detail2.getProductPrice())
                        .createdAt(now)
                        .lastUpdatedAt(now)
                        .build()
        ));
        entity.setTotalPrice((float) request.getOrderDetails().stream().mapToDouble(d -> d.getQuantity() * d.getProductPrice()).sum());

        OrderEntity savedEntity = OrderEntity.builder()
                .id(savedOrderId)
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .customerEmail(entity.getCustomerEmail())
                .totalPrice((float) request.getOrderDetails().stream().mapToDouble(d -> d.getQuantity() * d.getProductPrice()).sum())
                .createdAt(now)
                .lastUpdatedAt(now)
                .build();
        savedEntity.setOrderDetails(Set.of(
                OrderDetailEntity.builder()
                        .order(savedEntity)
                        .productId(detail1.getProductId())
                        .productName(detail1.getProductName())
                        .productPrice(detail1.getProductPrice())
                        .quantity(detail1.getQuantity())
                        .totalPrice(detail1.getQuantity() * detail1.getProductPrice())
                        .createdAt(now)
                        .lastUpdatedAt(now)
                        .build(),
                OrderDetailEntity.builder()
                        .order(savedEntity)
                        .productId(detail2.getProductId())
                        .productName(detail2.getProductName())
                        .productPrice(detail2.getProductPrice())
                        .quantity(detail2.getQuantity())
                        .totalPrice(detail2.getQuantity() * detail2.getProductPrice())
                        .createdAt(now)
                        .lastUpdatedAt(now)
                        .build()
        ));
        savedEntity.setTotalPrice((float) entity.getOrderDetails().stream().mapToDouble(d -> d.getQuantity() * d.getProductPrice()).sum());

        when(orderRepository.save(entity)).thenReturn(savedEntity);

        OrderResponseDto response = orderService.createOrder(request);

        assertEquals(response.getCustomerId(), savedEntity.getCustomerId());
        assertEquals(response.getCustomerName(), savedEntity.getCustomerName());
        assertEquals(response.getCustomerEmail(), savedEntity.getCustomerEmail());


        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getLastUpdatedAt());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getLastUpdatedAt());

        assertNotNull(savedEntity.getOrderDetails());
        assertNotNull(response.getOrderDetails());
        assertEquals(response.getOrderDetails().size(), savedEntity.getOrderDetails().size());

        float actualTotal = 0f;
        for (OrderDetailEntity detail : savedEntity.getOrderDetails()) {
            assertSame(savedEntity, detail.getOrder());
            assertNotNull(detail.getCreatedAt());
            assertNotNull(detail.getLastUpdatedAt());
            assertNotNull(detail.getTotalPrice());

            float expectedLineTotal = detail.getProductPrice() * detail.getQuantity();
            assertEquals(expectedLineTotal, detail.getTotalPrice());
            actualTotal += detail.getTotalPrice();
        }

        assertEquals(response.getTotalPrice(), actualTotal);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publishOrderCreatedEvent(eventCaptor.capture());

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(response.getId(), publishedEvent.getOrderId());
        assertEquals(response.getCustomerEmail(), publishedEvent.getCustomerEmail());
    }

    @Test
    void getOrderById_shouldReturnMappedResponse_whenFound() {
        UUID orderId = UUID.randomUUID();

        OrderEntity entity = new OrderEntity();
        entity.setId(orderId);
        entity.setCustomerId(UUID.randomUUID());
        entity.setCustomerName("Mohammad");
        entity.setCustomerEmail("mohammad@example.com");
        entity.setTotalPrice(500f);

        when(orderRepository.findByIdWithOrderDetails(orderId)).thenReturn(Optional.of(entity));

        OrderResponseDto response = orderService.getOrderById(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("Mohammad", response.getCustomerName());
        assertEquals("mohammad@example.com", response.getCustomerEmail());
        assertEquals(500f, response.getTotalPrice());

        verify(orderRepository).findByIdWithOrderDetails(orderId);
        verifyNoInteractions(orderEventPublisher);
    }

    @Test
    void getOrderById_shouldThrowException_whenNotFound() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdWithOrderDetails(orderId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> orderService.getOrderById(orderId)
        );

        assertEquals("Order not found with id: " + orderId, exception.getMessage());

        verify(orderRepository).findByIdWithOrderDetails(orderId);
        verifyNoInteractions(orderEventPublisher);
    }

    @Test
    void getAllOrders_shouldReturnMappedResponses() {
        OrderEntity entity1 = new OrderEntity();
        entity1.setId(UUID.randomUUID());
        entity1.setCustomerName("Customer 1");
        entity1.setCustomerEmail("c1@test.com");
        entity1.setTotalPrice(100f);

        OrderEntity entity2 = new OrderEntity();
        entity2.setId(UUID.randomUUID());
        entity2.setCustomerName("Customer 2");
        entity2.setCustomerEmail("c2@test.com");
        entity2.setTotalPrice(200f);

        when(orderRepository.findAll()).thenReturn(List.of(entity1, entity2));

        List<OrderResponseDto> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Customer 1", result.get(0).getCustomerName());
        assertEquals("Customer 2", result.get(1).getCustomerName());

        verify(orderRepository).findAll();
        verifyNoInteractions(orderEventPublisher);
    }
}
