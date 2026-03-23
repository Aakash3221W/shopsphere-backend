package com.shopsphere.orderservice.feign;

import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.orderservice.dto.ProductResponse;
import com.shopsphere.orderservice.feign.fallback.CatalogFeignClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "catalog-service",
        fallbackFactory = CatalogFeignClientFallbackFactory.class
)
public interface CatalogFeignClient {

    @GetMapping("/catalog/products/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable Long id);

    @PutMapping("/catalog/admin/products/{id}/stock")
    ApiResponse<Void> reduceStock(@PathVariable Long id,
                                   @RequestParam int quantity);
}