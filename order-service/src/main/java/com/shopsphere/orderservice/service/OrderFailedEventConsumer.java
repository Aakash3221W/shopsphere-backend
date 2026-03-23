package com.shopsphere.orderservice.service;

import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.orderservice.event.OrderFailedEvent;
import com.shopsphere.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFailedEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "order.failed",
            groupId = "order-service-group")
    @Transactional
    public void handleOrderFailed(OrderFailedEvent event) {
        log.warn("Received order.failed event for order {} — reason: {}",
                event.getOrderId(), event.getReason());

        orderRepository.findById(event.getOrderId())
                .ifPresent(order -> {
                    order.setStatus(OrderStatus.FAILED);
                    order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                    orderRepository.save(order);
                    log.warn("Order {} rolled back to FAILED — reason: {}",
                            event.getOrderId(), event.getReason());
                });
    }
}