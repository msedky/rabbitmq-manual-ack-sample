package com.rabbitmqsample.notificationservice.service.impl;

import com.rabbitmqsample.notificationservice.exception.NotFoundException;
import com.rabbitmqsample.notificationservice.mapper.NotificationMapper;
import com.rabbitmqsample.notificationservice.model.document.NotificationDocument;
import com.rabbitmqsample.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqsample.notificationservice.model.enums.NotificationStatus;
import com.rabbitmqsample.notificationservice.model.event.OrderCreatedEvent;
import com.rabbitmqsample.notificationservice.repository.NotificationRepository;
import com.rabbitmqsample.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public void createNotification(OrderCreatedEvent event) {

        NotificationDocument document = new NotificationDocument();
        document.setOrderId(event.getOrderId().toString());
        document.setCustomerEmail(event.getCustomerEmail());
        document.setType("EMAIL");
        document.setMessage("Order created successfully for customer " + event.getCustomerEmail());
        document.setStatus(NotificationStatus.RECEIVED);
        document.setCreatedAt(Instant.now());
        log.info("creating Notification " + document + " ....");
        notificationRepository.save(document);
    }

    @Override
    public NotificationResponseDto getById(String id) {
        log.info("getting Notification By Id" + id);
        NotificationDocument document = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found with id: " + id));

        return notificationMapper.toResponseDto(document);
    }

    @Override
    public List<NotificationResponseDto> getAll() {
        log.info("getting All Notifications");
        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<NotificationResponseDto> getByOrderId(String orderId) {
        log.info("getting Notification By Order Id" + orderId);
        return notificationRepository.findByOrderId(orderId)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<NotificationResponseDto> getByCustomerEmail(String customerEmail) {
        log.info("getting Notification By Customer Email" + customerEmail);
        return notificationRepository.findByCustomerEmail(customerEmail)
                .stream()
                .map(notificationMapper::toResponseDto)
                .toList();
    }
}