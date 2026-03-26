package com.shopsphere.adminservice.service;

import com.shopsphere.adminservice.dto.*;
import com.shopsphere.adminservice.feign.AuthFeignClient;
import com.shopsphere.adminservice.feign.CatalogFeignClient;
import com.shopsphere.adminservice.feign.OrderFeignClient;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.dto.UserDTO;
import com.shopsphere.common.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderFeignClient orderFeignClient;
    private final CatalogFeignClient catalogFeignClient;
    private final AuthFeignClient authFeignClient;

    // ─── Dashboard ───────────────────────────────────────────────────────────

    @Cacheable(value = "dashboard")
    public ApiResponse<DashboardDTO> getDashboard() {
        log.info("Generating Admin Dashboard metrics...");

        // Note: Pulling 1000 orders into memory is a temporary fix. 
        // In production, OrderService should provide a /stats endpoint.
        ApiResponse<PagedResponse<OrderResponse>> ordersResponse = 
                orderFeignClient.getAllOrders(0, 1000);

        long totalOrders = 0;
        long pendingOrders = 0;
        long cancelledOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        if (ordersResponse != null && ordersResponse.getData() != null) {
            totalOrders = ordersResponse.getData().getTotalElements();
            var orders = ordersResponse.getData().getContent();

            for (OrderResponse order : orders) {
                OrderStatus status = order.getStatus();
                
                // Grouping Logic for Dashboard
                if (status == OrderStatus.PAID || status == OrderStatus.PACKED) {
                    pendingOrders++;
                }
                
                if (status == OrderStatus.CANCELLED) {
                    cancelledOrders++;
                }

                // Revenue calculation (Only count completed or active revenue)
                if (isRevenueGenerating(status)) {
                    BigDecimal amount = (order.getTotalAmount() != null) ? order.getTotalAmount() : BigDecimal.ZERO;
                    totalRevenue = totalRevenue.add(amount);
                }
            }
        }

        long totalUsers = fetchUserCountSafely();

        DashboardDTO dashboard = DashboardDTO.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalRevenue(totalRevenue)
                .totalUsers(totalUsers)
                .cancelledOrders(cancelledOrders)
                .build();

        return ApiResponse.success(dashboard);
    }

    // ─── Orders, Products, Users ─────────────────────────────────────────────
    // These are simple pass-throughs to Feign. They look good!

    @Cacheable(value = "adminOrders", key = "#page + '-' + #size")
    public ApiResponse<PagedResponse<OrderResponse>> getAllOrders(int page, int size) {
        return orderFeignClient.getAllOrders(page, size);
    }

    @Cacheable(value = "adminOrder", key = "#id")
    public ApiResponse<OrderResponse> getOrderById(Long id) {
        return orderFeignClient.getOrderById(id);
    }

    @CacheEvict(value = {"adminOrders", "adminOrder", "dashboard"}, allEntries = true)
    public ApiResponse<OrderResponse> updateOrderStatus(Long id, OrderStatus status) {
        log.info("Admin Action: Updating order {} status to {}", id, status);
        return orderFeignClient.updateOrderStatus(id, status);
    }

    public ApiResponse<ProductResponse> createProduct(ProductRequest request) {
        return catalogFeignClient.createProduct(request);
    }

    public ApiResponse<ProductResponse> updateProduct(Long id, ProductRequest request) {
        return catalogFeignClient.updateProduct(id, request);
    }

    public ApiResponse<Void> deleteProduct(Long id) {
        return catalogFeignClient.deleteProduct(id);
    }

    @Cacheable(value = "adminUsers", key = "#page + '-' + #size")
    public ApiResponse<PagedResponse<UserDTO>> getAllUsers(int page, int size) {
        return authFeignClient.getAllUsers(page, size);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private boolean isRevenueGenerating(OrderStatus status) {
        return status == OrderStatus.PAID || 
               status == OrderStatus.PACKED || 
               status == OrderStatus.SHIPPED || 
               status == OrderStatus.DELIVERED;
    }

    private long fetchUserCountSafely() {
        try {
            ApiResponse<PagedResponse<UserDTO>> usersResponse = authFeignClient.getAllUsers(0, 1);
            if (usersResponse != null && usersResponse.getData() != null) {
                return usersResponse.getData().getTotalElements();
            }
        } catch (Exception e) {
            log.warn("Dashboard alert: Failed to fetch user count from Auth Service: {}", e.getMessage());
        }
        return 0;
    }
    public ApiResponse<ProductResponse> updateStock(Long id, int quantity) {
        return catalogFeignClient.updateStock(id, quantity);
    }
}