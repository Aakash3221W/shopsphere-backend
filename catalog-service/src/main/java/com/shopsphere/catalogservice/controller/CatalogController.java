package com.shopsphere.catalogservice.controller;

import com.shopsphere.catalogservice.dto.*;
import com.shopsphere.catalogservice.service.CatalogService;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    // ─── Public Endpoints ────────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(catalogService.getAllProducts(
                search, categoryId, minPrice, maxPrice, sort, page, size));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getProductById(id));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getFeaturedProducts() {
        return ResponseEntity.ok(catalogService.getFeaturedProducts());
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    @GetMapping("/categories/{id}/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDTO>>> getProductsByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(
                catalogService.getProductsByCategory(id, page, size));
    }

    // ─── Admin Endpoints ─────────────────────────────────────────────────────

    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(catalogService.createProduct(request));
    }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(catalogService.updateProduct(id, request));
    }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id) {
        return ResponseEntity.ok(catalogService.deleteProduct(id));
    }

    @PutMapping("/admin/products/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductDTO>> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        return ResponseEntity.ok(catalogService.updateStock(id, quantity));
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(catalogService.createCategory(request));
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(catalogService.updateCategory(id, request));
    }
}