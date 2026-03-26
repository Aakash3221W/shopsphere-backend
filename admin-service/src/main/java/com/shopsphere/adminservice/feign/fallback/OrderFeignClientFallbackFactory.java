package com.shopsphere.adminservice.feign.fallback;

import com.shopsphere.adminservice.dto.OrderResponse;
import com.shopsphere.adminservice.feign.OrderFeignClient;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.enums.OrderStatus;
import com.shopsphere.common.exception.ServiceUnavailableException; // Ensure this is imported
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderFeignClientFallbackFactory
        implements FallbackFactory<OrderFeignClient> {

    @Override
    public OrderFeignClient create(Throwable cause) {
        return new OrderFeignClient() {

            @Override
            public ApiResponse<PagedResponse<OrderResponse>> getAllOrders(
                    int page, int size) {
                log.error("Order Service failure — getAllOrders: {}", cause.getMessage());
                throw new ServiceUnavailableException(
                        "Order Service is currently unavailable. Unable to fetch order list.");
            }

            @Override
            public ApiResponse<OrderResponse> getOrderById(Long id) {
                log.error("Order Service failure — getOrderById({}): {}", id, cause.getMessage());
                throw new ServiceUnavailableException(
                        "Order Service is currently unavailable. Cannot retrieve order " + id);
            }

            @Override
            public ApiResponse<OrderResponse> updateOrderStatus(
                    Long id, OrderStatus status) {
                log.error("Order Service failure — updateOrderStatus for {}: {}", id, cause.getMessage());
                throw new ServiceUnavailableException(
                        "Order Service is currently unavailable. Status update failed for order " + id);
            }
        };
    }
}