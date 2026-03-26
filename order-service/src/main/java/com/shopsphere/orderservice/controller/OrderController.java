package com.shopsphere.orderservice.controller;

import com.shopsphere.common.constants.AppConstants;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.orderservice.dto.*;
import com.shopsphere.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ─── Cart Management ──────────────────────────────────────────────────────

    @GetMapping("/cart")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId) {
        return ResponseEntity.ok(orderService.getCart(userId));
    }

    @PostMapping("/cart/items")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(orderService.addToCart(userId, request));
    }

    @PutMapping("/cart/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> updateCartItem(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(orderService.updateCartItem(userId, itemId, request));
    }

    @DeleteMapping("/cart/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> removeCartItem(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(orderService.removeCartItem(userId, itemId));
    }

    @DeleteMapping("/cart")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId) {
        return ResponseEntity.ok(orderService.clearCart(userId));
    }

    // ─── Checkout & Placement ────────────────────────────────────────────────

    @PostMapping("/checkout/start")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> startCheckout(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId) {
        return ResponseEntity.ok(orderService.startCheckout(userId));
    }

    @PostMapping("/checkout/address")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> saveAddress(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long orderId,
            @RequestParam String address) {
        return ResponseEntity.ok(orderService.saveAddress(userId, orderId, address));
    }

    @PostMapping("/checkout/delivery")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> selectDelivery(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long orderId,
            @Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(orderService.selectDelivery(userId, orderId, request));
    }

    /**
     * Finalizes payment and places the order.
     */
    @PostMapping("/place")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> placeOrder(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @RequestParam Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        // Pointing to the refined service method that handles stock reduction
        return ResponseEntity.ok(orderService.processPayment(userId, orderId, request));
    }

    // ─── Order History (User Context) ────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getMyOrders(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId) {
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(userId, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @RequestHeader(AppConstants.HEADER_USER_ID) Long userId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }

 // ─── Admin Operations ────────────────────────────────────────────────────

    // Path matched to AdminService: @GetMapping("/orders/admin/orders")
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    // Path matched to AdminService: @GetMapping("/orders/admin/orders/{id}")
    @GetMapping("/admin/orders/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByIdAdmin(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDTOById(orderId));
    }

    // Path matched to AdminService: @PutMapping("/orders/admin/orders/{id}/status")
    @PutMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, status));
    }
}