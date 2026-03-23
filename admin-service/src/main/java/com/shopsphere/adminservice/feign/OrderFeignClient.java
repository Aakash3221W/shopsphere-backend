package com.shopsphere.adminservice.feign;

import com.shopsphere.adminservice.feign.fallback.OrderFeignClientFallbackFactory;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.adminservice.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "order-service",
        fallbackFactory = OrderFeignClientFallbackFactory.class
)
public interface OrderFeignClient {

    @GetMapping("/orders/admin/orders")
    ApiResponse<PagedResponse<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);

    @GetMapping("/orders/admin/orders/{id}")
    ApiResponse<OrderResponse> getOrderById(@PathVariable Long id);

    @PutMapping("/orders/admin/orders/{id}/status")
    ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status);
}