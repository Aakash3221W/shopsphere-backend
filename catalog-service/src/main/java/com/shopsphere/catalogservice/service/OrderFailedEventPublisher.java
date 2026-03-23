package com.shopsphere.catalogservice.service;

import com.shopsphere.catalogservice.event.OrderFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFailedEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderFailed(Long orderId, Long userId, String reason) {
        OrderFailedEvent event = OrderFailedEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .reason(reason)
                .build();

        kafkaTemplate.send("order.failed",
                String.valueOf(orderId), event);
        log.warn("Published order.failed event for order {} — reason: {}",
                orderId, reason);
    }
}