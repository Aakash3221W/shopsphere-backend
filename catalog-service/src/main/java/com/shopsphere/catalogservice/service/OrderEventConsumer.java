package com.shopsphere.catalogservice.service;
import com.shopsphere.catalogservice.entity.Product;
import com.shopsphere.catalogservice.event.OrderItemEvent;
import com.shopsphere.catalogservice.event.OrderPlacedEvent;
import com.shopsphere.catalogservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductRepository productRepository;
    private final OrderFailedEventPublisher orderFailedEventPublisher;

    @KafkaListener(
            topics = "order.placed",
            groupId = "catalog-service-group")
    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed event for order {}",
                event.getOrderId());

        for (OrderItemEvent item : event.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElse(null);

            if (product == null) {
                log.error("Product {} not found — failing order {}",
                        item.getProductId(), event.getOrderId());
                orderFailedEventPublisher.publishOrderFailed(
                        event.getOrderId(),
                        event.getUserId(),
                        "Product " + item.getProductId() + " not found");
                return;
            }

            int newStock = product.getStockQuantity() - item.getQuantity();

            if (newStock < 0) {
                log.warn("Insufficient stock for product {} — failing order {}",
                        item.getProductId(), event.getOrderId());
                orderFailedEventPublisher.publishOrderFailed(
                        event.getOrderId(),
                        event.getUserId(),
                        "Insufficient stock for product: " + product.getName());
                return;
            }

            product.setStockQuantity(newStock);
            productRepository.save(product);
            log.info("Stock reduced for product {} — new stock: {}",
                    item.getProductId(), newStock);
        }
    }

    @KafkaListener(
            topics = "order.cancelled",
            groupId = "catalog-service-group")
    @Transactional
    public void handleOrderCancelled(OrderPlacedEvent event) {
        log.info("Received order.cancelled event for order {}",
                event.getOrderId());

        for (OrderItemEvent item : event.getItems()) {
            productRepository.findById(item.getProductId())
                    .ifPresent(product -> {
                        int newStock = product.getStockQuantity()
                                + item.getQuantity();
                        product.setStockQuantity(newStock);
                        productRepository.save(product);
                        log.info("Stock restored for product {} — new stock: {}",
                                item.getProductId(), newStock);
                    });
        }
    }
}