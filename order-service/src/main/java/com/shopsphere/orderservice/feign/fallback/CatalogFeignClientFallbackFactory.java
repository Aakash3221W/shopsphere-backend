package com.shopsphere.orderservice.feign.fallback;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.exception.ServiceUnavailableException;
import com.shopsphere.orderservice.dto.ProductResponse;
import com.shopsphere.orderservice.feign.CatalogFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CatalogFeignClientFallbackFactory
        implements FallbackFactory<CatalogFeignClient> {

    @Override
    public CatalogFeignClient create(Throwable cause) {
        return new CatalogFeignClient() {

            @Override
            public ApiResponse<ProductResponse> getProductById(Long id) {
                log.error("Catalog Service unavailable — getProductById({}): {}",
                        id, cause.getMessage());
                throw new ServiceUnavailableException(
                        "Product service temporarily unavailable. Please try again.");
            }

            @Override
            public ApiResponse<Void> reduceStock(Long id, int quantity) {
                log.error("Catalog Service unavailable — reduceStock({}, {}): {}",
                        id, quantity, cause.getMessage());
                throw new ServiceUnavailableException(
                        "Unable to finalize order due to stock service issues.");
            }
        };
    }
}