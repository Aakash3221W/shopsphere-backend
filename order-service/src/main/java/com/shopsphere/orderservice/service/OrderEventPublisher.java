package com.shopsphere.orderservice.service;

import com.shopsphere.orderservice.event.OrderItemEvent;
import com.shopsphere.orderservice.event.OrderPlacedEvent;
import com.shopsphere.orderservice.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderPlaced(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(i -> OrderItemEvent.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        kafkaTemplate.send("order.placed", String.valueOf(order.getId()), event);
        log.info("Published order.placed event for order {}", order.getId());
    }

    public void publishOrderCancelled(Order order) {
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(order.getItems().stream()
                        .map(i -> OrderItemEvent.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        kafkaTemplate.send("order.cancelled", String.valueOf(order.getId()), event);
        log.info("Published order.cancelled event for order {}", order.getId());
    }
}