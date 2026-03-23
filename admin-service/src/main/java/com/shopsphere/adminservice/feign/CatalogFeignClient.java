package com.shopsphere.adminservice.feign;

import com.shopsphere.adminservice.dto.ProductRequest;
import com.shopsphere.adminservice.dto.ProductResponse;
import com.shopsphere.adminservice.feign.fallback.CatalogFeignClientFallbackFactory;
import com.shopsphere.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "catalog-service",
        fallbackFactory = CatalogFeignClientFallbackFactory.class
)
public interface CatalogFeignClient {

    @PostMapping("/catalog/admin/products")
    ApiResponse<ProductResponse> createProduct(@RequestBody ProductRequest request);

    @PutMapping("/catalog/admin/products/{id}")
    ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request);

    @DeleteMapping("/catalog/admin/products/{id}")
    ApiResponse<Void> deleteProduct(@PathVariable Long id);

    @PutMapping("/catalog/admin/products/{id}/stock")
    ApiResponse<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity);

    @GetMapping("/catalog/products")
    ApiResponse<Object> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size);
    
}