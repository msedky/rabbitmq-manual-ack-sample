package com.rabbitmqsample.notificationservice.model.document;

import com.rabbitmqsample.notificationservice.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDocument {

    @Id
    private String id;
    @Indexed
    private String orderId;
    private String customerEmail;
    private String type;
    private String message;
    private NotificationStatus status;
    private Instant createdAt;
}