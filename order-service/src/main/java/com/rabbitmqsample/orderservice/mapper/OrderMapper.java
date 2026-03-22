package com.rabbitmqsample.orderservice.mapper;

import com.rabbitmqsample.orderservice.model.dto.request.CreateOrderRequestDto;
import com.rabbitmqsample.orderservice.model.dto.response.OrderResponseDto;
import com.rabbitmqsample.orderservice.model.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = OrderDetailMapper.class)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    OrderEntity toEntity(CreateOrderRequestDto dto);

    OrderResponseDto toResponseDto(OrderEntity entity);
}