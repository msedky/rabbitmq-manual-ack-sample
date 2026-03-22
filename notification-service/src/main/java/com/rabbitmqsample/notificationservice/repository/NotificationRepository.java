package com.rabbitmqsample.notificationservice.repository;

import com.rabbitmqsample.notificationservice.model.document.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
    List<NotificationDocument> findByOrderId(String orderId);
    List<NotificationDocument> findByCustomerEmail(String customerEmail);
}