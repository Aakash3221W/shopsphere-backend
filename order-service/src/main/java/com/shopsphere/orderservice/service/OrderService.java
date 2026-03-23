package com.shopsphere.orderservice.service;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.enums.PaymentStatus;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.entity.*;
import com.shopsphere.orderservice.feign.CatalogFeignClient;
import com.shopsphere.orderservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CatalogFeignClient catalogFeignClient;
    private final OrderEventPublisher orderEventPublisher;

    // ─── Cart Operations ─────────────────────────────────────────────────────

    public ApiResponse<CartDTO> getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return ApiResponse.success(toCartDTO(cart));
    }

    @Transactional
    public ApiResponse<CartDTO> addToCart(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductResponse product = catalogFeignClient
                .getProductById(request.getProductId())
                .getData();

        if (product == null || !product.isActive()) {
            throw new BadRequestException("Product is not available");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException(
                    "Insufficient stock. Available: " + product.getStockQuantity());
        }

        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .productId(product.getId())
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(request.getQuantity())
                    .build());
        }

        log.info("Item added to cart for user {}", userId);
        return ApiResponse.success("Item added to cart",
                toCartDTO(cartRepository.save(cart)));
    }

    @Transactional
    public ApiResponse<CartDTO> updateCartItem(Long userId,
            Long itemId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId));

        item.setQuantity(request.getQuantity());
        return ApiResponse.success("Cart item updated",
                toCartDTO(cartRepository.save(cart)));
    }

    @Transactional
    public ApiResponse<CartDTO> removeCartItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return ApiResponse.success("Item removed from cart",
                toCartDTO(cartRepository.save(cart)));
    }

    @Transactional
    public ApiResponse<Void> clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
        return ApiResponse.success("Cart cleared", null);
    }

    // ─── Checkout & Payment ──────────────────────────────────────────────────

    @Transactional
    public ApiResponse<OrderDTO> startCheckout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout with empty cart");
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CHECKOUT)
                .totalAmount(calculateItemsTotal(cart.getItems()))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(i -> OrderItem.builder()
                        .order(order)
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getUnitPrice()
                                .multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        order.setItems(orderItems);
        log.info("Checkout started for user {} — order {}",
                userId, order.getId());
        return ApiResponse.success("Checkout started",
                toOrderDTO(orderRepository.save(order)));
    }

    @Transactional
    public ApiResponse<OrderDTO> processPayment(Long userId,
            Long orderId, PaymentRequest request) {
        Order order = getOrderForUser(userId, orderId);
        validateOrderEditable(order);

        order.setPaymentMode(request.getPaymentMode());
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.PAID);
        order.setPlacedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        // Publish Kafka event — Catalog Service reduces stock asynchronously
        orderEventPublisher.publishOrderPlaced(saved);

        clearCart(userId);

        log.info("Order {} placed successfully for user {}", orderId, userId);
        return ApiResponse.success("Order placed successfully", toOrderDTO(saved));
    }

    // ─── Address & Delivery ──────────────────────────────────────────────────

    @Transactional
    public ApiResponse<OrderDTO> saveAddress(Long userId,
            Long orderId, String address) {
        Order order = getOrderForUser(userId, orderId);
        validateOrderEditable(order);
        order.setShippingAddress(address);
        return ApiResponse.success("Address saved",
                toOrderDTO(orderRepository.save(order)));
    }

    @Transactional
    public ApiResponse<OrderDTO> selectDelivery(Long userId,
            Long orderId, CheckoutRequest request) {
        Order order = getOrderForUser(userId, orderId);
        validateOrderEditable(order);
        order.setShippingAddress(request.getShippingAddress());
        order.setDeliveryMethod(request.getDeliveryMethod());
        return ApiResponse.success("Delivery method saved",
                toOrderDTO(orderRepository.save(order)));
    }

    // ─── Order History & Admin ───────────────────────────────────────────────

    public ApiResponse<List<OrderDTO>> getMyOrders(Long userId) {
        return ApiResponse.success(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        .stream().map(this::toOrderDTO)
                        .collect(Collectors.toList()));
    }

    public ApiResponse<OrderDTO> getOrderById(Long userId, Long orderId) {
        return ApiResponse.success(
                toOrderDTO(getOrderForUser(userId, orderId)));
    }

    public ApiResponse<OrderDTO> getOrderDTOById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", orderId));
        return ApiResponse.success(toOrderDTO(order));
    }

    @Transactional
    public ApiResponse<OrderDTO> updateOrderStatus(Long orderId,
            OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", orderId));
        order.setStatus(status);
        log.info("Order {} status updated to {}", orderId, status);
        return ApiResponse.success("Status updated",
                toOrderDTO(orderRepository.save(order)));
    }

    @Transactional
    public ApiResponse<OrderDTO> cancelOrder(Long userId, Long orderId) {
        Order order = getOrderForUser(userId, orderId);
        if (order.getStatus() != OrderStatus.CHECKOUT &&
            order.getStatus() != OrderStatus.PAID) {
            throw new BadRequestException(
                    "Order cannot be cancelled in status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Publish Kafka event — Catalog Service restores stock asynchronously
        orderEventPublisher.publishOrderCancelled(saved);

        log.info("Order {} cancelled by user {}", orderId, userId);
        return ApiResponse.success("Order cancelled", toOrderDTO(saved));
    }

    public ApiResponse<PagedResponse<OrderDTO>> getAllOrders(
            int page, int size) {
        Page<Order> orders = orderRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        List<OrderDTO> content = orders.getContent().stream()
                .map(this::toOrderDTO).collect(Collectors.toList());
        return ApiResponse.success(new PagedResponse<>(
                content, orders.getNumber(), orders.getSize(),
                orders.getTotalElements(), orders.getTotalPages(),
                orders.isLast()));
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).build()));
    }

    private Order getOrderForUser(Long userId, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", orderId));
    }

    private void validateOrderEditable(Order order) {
        List<OrderStatus> nonEditable = List.of(
                OrderStatus.PAID, OrderStatus.SHIPPED,
                OrderStatus.DELIVERED, OrderStatus.CANCELLED);
        if (nonEditable.contains(order.getStatus())) {
            throw new BadRequestException(
                    "Order cannot be modified in status: " +
                    order.getStatus());
        }
    }

    private BigDecimal calculateItemsTotal(List<CartItem> items) {
        return items.stream()
                .map(i -> i.getUnitPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderDTO toOrderDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .deliveryMethod(order.getDeliveryMethod())
                .paymentMode(order.getPaymentMode())
                .paymentStatus(order.getPaymentStatus())
                .placedAt(order.getPlacedAt())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(i -> OrderItemDTO.builder()
                                .id(i.getId())
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .unitPrice(i.getUnitPrice())
                                .quantity(i.getQuantity())
                                .lineTotal(i.getLineTotal())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private CartDTO toCartDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream()
                .map(i -> CartItemDTO.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getUnitPrice()
                                .multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(items)
                .totalAmount(items.stream()
                        .map(CartItemDTO::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .totalItems(items.size())
                .build();
    }
}