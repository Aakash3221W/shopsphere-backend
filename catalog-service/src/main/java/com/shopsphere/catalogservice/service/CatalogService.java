package com.shopsphere.catalogservice.service;

import com.shopsphere.catalogservice.dto.*;
import com.shopsphere.catalogservice.entity.Category;
import com.shopsphere.catalogservice.entity.Product;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import com.shopsphere.common.constants.AppConstants;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.dto.PagedResponse;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ConflictException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ─── Public — Products ───────────────────────────────────────────────────

    @Cacheable(value = "products", key = "#search + '-' + #categoryId + '-' + #minPrice + '-' + #maxPrice + '-' + #sort + '-' + #page + '-' + #size")
    public ApiResponse<PagedResponse<ProductDTO>> getAllProducts(
            String search, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sort, int page, int size) {

        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));

        Page<Product> products = productRepository.searchProducts(
                search, categoryId, minPrice, maxPrice, pageable);

        return ApiResponse.success(toPagedResponse(products));
    }

    @Cacheable(value = "product", key = "#id")
    public ApiResponse<ProductDTO> getProductById(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ApiResponse.success(toDTO(product));
    }

    @Cacheable(value = "featuredProducts")
    public ApiResponse<List<ProductDTO>> getFeaturedProducts() {
        List<ProductDTO> featured = productRepository
                .findByIsFeaturedTrueAndIsActiveTrue()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ApiResponse.success(featured);
    }

    @Cacheable(value = "productsByCategory", key = "#categoryId + '-' + #page + '-' + #size")
    public ApiResponse<PagedResponse<ProductDTO>> getProductsByCategory(
            Long categoryId, int page, int size) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository
                .findByCategoryIdAndIsActiveTrue(categoryId, pageable);

        return ApiResponse.success(toPagedResponse(products));
    }

    // ─── Public — Categories ─────────────────────────────────────────────────

    @Cacheable(value = "categories")
    public ApiResponse<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryRepository
                .findByIsActiveTrue()
                .stream()
                .map(this::toCategoryDTO)
                .collect(Collectors.toList());
        return ApiResponse.success(categories);
    }

    // ─── Admin — Products ────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"products", "productsByCategory", "featuredProducts"}, allEntries = true)
    public ApiResponse<ProductDTO> createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .category(category)
                .isFeatured(request.isFeatured())
                .isActive(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created: {}", saved.getId());
        return ApiResponse.success("Product created successfully", toDTO(saved));
    }

    @Transactional
    @CacheEvict(value = {"products", "product", "productsByCategory", "featuredProducts"}, allEntries = true)
    public ApiResponse<ProductDTO> updateProduct(Long id,
            CreateProductRequest request) {

        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);
        product.setFeatured(request.isFeatured());

        Product saved = productRepository.save(product);
        log.info("Product updated: {}", saved.getId());
        return ApiResponse.success("Product updated successfully", toDTO(saved));
    }

    @Transactional
    @CacheEvict(value = {"products", "product", "productsByCategory", "featuredProducts"}, allEntries = true)
    public ApiResponse<Void> deleteProduct(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft deleted: {}", id);
        return ApiResponse.success("Product deleted successfully", null);
    }

    @Transactional
    @CacheEvict(value = "product", key = "#id")
    public ApiResponse<ProductDTO> updateStock(Long id, int quantity) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (quantity < 0) {
            throw new BadRequestException("Stock quantity cannot be negative");
        }

        product.setStockQuantity(quantity);
        Product saved = productRepository.save(product);
        log.info("Stock updated for product {}: {}", id, quantity);
        return ApiResponse.success("Stock updated successfully", toDTO(saved));
    }

    // ─── Admin — Categories ──────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse<CategoryDTO> createCategory(
            CreateCategoryRequest request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException(
                    "Category already exists: " + request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: {}", saved.getId());
        return ApiResponse.success("Category created successfully",
                toCategoryDTO(saved));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ApiResponse<CategoryDTO> updateCategory(Long id,
            CreateCategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        Category saved = categoryRepository.save(category);
        log.info("Category updated: {}", saved.getId());
        return ApiResponse.success("Category updated successfully",
                toCategoryDTO(saved));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Sort buildSort(String sort) {
        if (sort == null) return Sort.by("createdAt").descending();
        return switch (sort) {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "name_asc"   -> Sort.by("name").ascending();
            case "newest"     -> Sort.by("createdAt").descending();
            default           -> Sort.by("createdAt").descending();
        };
    }

    private ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .originalPrice(p.getOriginalPrice())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .featured(p.isFeatured())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private CategoryDTO toCategoryDTO(Category c) {
        return CategoryDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .active(c.isActive())
                .build();
    }

    private PagedResponse<ProductDTO> toPagedResponse(Page<Product> page) {
        List<ProductDTO> content = page.getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}