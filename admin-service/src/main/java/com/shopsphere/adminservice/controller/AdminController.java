package com.shopsphere.adminservice.controller;

import com.shopsphere.adminservice.dto.*;
import com.shopsphere.adminservice.service.AdminService;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ─── Dashboard ───────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    // ─── Orders ──────────────────────────────────────────────────────────────
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllOrders(page, size));
    }

    @GetMapping("/orders/{orderId}") // Renamed to {orderId} for clarity
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(adminService.getOrderById(orderId));
    }

    @PutMapping("/orders/{orderId}/status") // Consistent naming
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(adminService.updateOrderStatus(orderId, status));
    }

    // ─── Products ────────────────────────────────────────────────────────────
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        // Return 201 Created for new resources
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createProduct(request));
    }

    @PutMapping("/products/{productId}") // Renamed to {productId}
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(adminService.updateProduct(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(adminService.deleteProduct(productId));
    }
    @PutMapping("/products/{productId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable("productId") Long productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(adminService.updateStock(productId, quantity));
    }

    // ─── Users ───────────────────────────────────────────────────────────────
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllUsers(page, size));
    }
}