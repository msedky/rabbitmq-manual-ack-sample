package com.rabbitmqsample.notificationservice.controller;

import com.rabbitmqsample.notificationservice.model.dto.response.NotificationResponseDto;
import com.rabbitmqsample.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<NotificationResponseDto>> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(notificationService.getByOrderId(orderId));
    }

    @GetMapping("/customer")
    public ResponseEntity<List<NotificationResponseDto>> getByCustomerEmail(@RequestParam String email) {
        return ResponseEntity.ok(notificationService.getByCustomerEmail(email));
    }
}