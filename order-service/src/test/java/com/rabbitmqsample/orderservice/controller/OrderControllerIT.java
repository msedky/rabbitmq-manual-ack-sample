package com.rabbitmqsample.orderservice.controller;

import com.rabbitmqsample.orderservice.exception.NotFoundException;
import com.rabbitmqsample.orderservice.model.dto.request.CreateOrderRequestDto;
import com.rabbitmqsample.orderservice.model.dto.request.OrderDetailRequestDto;
import com.rabbitmqsample.orderservice.model.dto.response.OrderResponseDto;
import com.rabbitmqsample.orderservice.model.entity.OrderDetailEntity;
import com.rabbitmqsample.orderservice.model.entity.OrderEntity;
import com.rabbitmqsample.orderservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.orderservice.repository.OrderDetailRepository;
import com.rabbitmqsample.orderservice.repository.OrderRepository;
import com.rabbitmqsample.orderservice.service.OrderEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class OrderControllerIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("orders_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @BeforeEach
    void setUp() {
        orderDetailRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_shouldPersistOrder_andReturnCreatedResponse() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID product1Id = UUID.randomUUID();
        UUID product2Id = UUID.randomUUID();

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

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.customerName").value(request.getCustomerName()))
                .andExpect(jsonPath("$.customerEmail").value(request.getCustomerEmail()))
                .andExpect(jsonPath("$.totalPrice").value(request.getOrderDetails().stream().mapToDouble(d -> d.getQuantity() * d.getProductPrice()).sum()))
                .andExpect(jsonPath("$.orderDetails", hasSize(request.getOrderDetails().size())))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.lastUpdatedAt").isNotEmpty())
                .andReturn();

        OrderResponseDto orderResponseDto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), OrderResponseDto.class);

        OrderEntity savedOrder = orderRepository.findByIdWithOrderDetails(orderResponseDto.getId()).orElseThrow(() -> new NotFoundException());
        assertEquals(customerId, savedOrder.getCustomerId());
        assertEquals(orderResponseDto.getCustomerName(), savedOrder.getCustomerName());
        assertEquals(orderResponseDto.getCustomerEmail(), savedOrder.getCustomerEmail());

        assertEquals(
                (float) orderResponseDto.getOrderDetails().stream().mapToDouble(d -> d.getQuantity() * d.getProductPrice()).sum()
                , savedOrder.getTotalPrice());

        verify(orderEventPublisher, times(1))
                .publishOrderCreatedEvent(OrderCreatedEvent.builder()
                        .orderId(savedOrder.getId())
                        .customerEmail(savedOrder.getCustomerEmail())
                        .build());

    }

    @Test
    void getOrderById_shouldReturnOrder_whenExists() throws Exception {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCustomerId(UUID.randomUUID());
        orderEntity.setCustomerName("Customer One");
        orderEntity.setCustomerEmail("customer1@test.com");
        orderEntity.setTotalPrice(500f);
        orderEntity.setOrderDetails(Set.of(
                OrderDetailEntity.builder()
                        .order(orderEntity)
                        .productId(UUID.randomUUID())
                        .productName("P1")
                        .productPrice(100.0f)
                        .quantity(2)
                        .totalPrice(100.0f * 2)
                        .build(),
                OrderDetailEntity.builder()
                        .order(orderEntity)
                        .productId(UUID.randomUUID())
                        .productName("P2")
                        .productPrice(75.0f)
                        .quantity(3)
                        .totalPrice(75.0f * 3)
                        .build(),
                OrderDetailEntity.builder()
                        .order(orderEntity)
                        .productId(UUID.randomUUID())
                        .productName("P3")
                        .productPrice(60.0f)
                        .quantity(1)
                        .totalPrice(60.0f * 1)
                        .build()
        ));
        Instant now = Instant.now();
        orderEntity.setCreatedAt(now);
        orderEntity.setLastUpdatedAt(now);

        OrderEntity savedOrder = orderRepository.save(orderEntity);

        mockMvc.perform(get("/api/v1/orders/{orderId}", savedOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(savedOrder.getCustomerId().toString()))
                .andExpect(jsonPath("$.customerName").value(savedOrder.getCustomerName()))
                .andExpect(jsonPath("$.customerEmail").value(savedOrder.getCustomerEmail()))
                .andExpect(jsonPath("$.totalPrice").value(savedOrder.getTotalPrice()))
                .andExpect(jsonPath("$.orderDetails", hasSize(savedOrder.getOrderDetails().size())))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.lastUpdatedAt").isNotEmpty());
    }

    @Test
    void getOrderById_shouldReturn500_whenOrderDoesNotExist() throws Exception {
        UUID notExistingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/orders/{orderId}", notExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllOrders_shouldReturnAllOrders() throws Exception {
        OrderEntity order1 = new OrderEntity();
        order1.setCustomerId(UUID.randomUUID());
        order1.setCustomerName("Customer One");
        order1.setCustomerEmail("customer1@test.com");
        order1.setTotalPrice(100f);

        OrderEntity order2 = new OrderEntity();
        order2.setCustomerId(UUID.randomUUID());
        order2.setCustomerName("Customer Two");
        order2.setCustomerEmail("customer2@test.com");
        order2.setTotalPrice(200f);

        orderRepository.save(order1);
        orderRepository.save(order2);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}