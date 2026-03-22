package com.rabbitmqsample.notificationservice.mapper;

import com.rabbitmqsample.notificationservice.model.document.NotificationDocument;
import com.rabbitmqsample.notificationservice.model.dto.response.NotificationResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponseDto toResponseDto(NotificationDocument document);
}