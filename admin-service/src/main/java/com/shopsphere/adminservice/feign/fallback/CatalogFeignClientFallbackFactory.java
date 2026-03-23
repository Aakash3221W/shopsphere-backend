package com.shopsphere.adminservice.feign.fallback;

import com.shopsphere.adminservice.dto.ProductRequest;
import com.shopsphere.adminservice.dto.ProductResponse;
import com.shopsphere.adminservice.feign.CatalogFeignClient;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.exception.ServiceUnavailableException; // Import our custom exception
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
            public ApiResponse<ProductResponse> createProduct(ProductRequest request) {
                log.error("Catalog Service unavailable — createProduct: {}", cause.getMessage());
                throw new ServiceUnavailableException("Catalog Service is currently down. Cannot create product.");
            }

            @Override
            public ApiResponse<ProductResponse> updateProduct(Long id, ProductRequest request) {
                log.error("Catalog Service unavailable — updateProduct({}): {}", id, cause.getMessage());
                throw new ServiceUnavailableException("Catalog Service is currently down. Cannot update product " + id);
            }

            @Override
            public ApiResponse<Void> deleteProduct(Long id) {
                log.error("Catalog Service unavailable — deleteProduct({}): {}", id, cause.getMessage());
                throw new ServiceUnavailableException("Catalog Service is currently down. Cannot delete product " + id);
            }

            @Override
            public ApiResponse<ProductResponse> updateStock(Long id, int quantity) {
                log.error("Catalog Service unavailable — updateStock({}): {}", id, cause.getMessage());
                throw new ServiceUnavailableException("Catalog Service is currently down. Cannot update stock for product " + id);
            }

            @Override
            public ApiResponse<Object> getAllProducts(int page, int size) {
                log.error("Catalog Service unavailable — getAllProducts: {}", cause.getMessage());
                throw new ServiceUnavailableException("Catalog Service is currently down. Cannot fetch products.");
            }
        };
    }
}