package com.rabbitmqsample.orderservice.mapper;

import com.rabbitmqsample.orderservice.model.dto.request.OrderDetailRequestDto;
import com.rabbitmqsample.orderservice.model.dto.response.OrderDetailResponseDto;
import com.rabbitmqsample.orderservice.model.entity.OrderDetailEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    OrderDetailEntity toEntity(OrderDetailRequestDto dto);

    OrderDetailResponseDto toResponseDto(OrderDetailEntity entity);
}