package com.shopsphere.catalogservice.service;

import com.shopsphere.catalogservice.dto.*;
import com.shopsphere.catalogservice.entity.Category;
import com.shopsphere.catalogservice.entity.Product;
import com.shopsphere.catalogservice.repository.CategoryRepository;
import com.shopsphere.catalogservice.repository.ProductRepository;
import com.shopsphere.common.dto.ApiResponse;
import com.shopsphere.common.exception.BadRequestException;
import com.shopsphere.common.exception.ConflictException;
import com.shopsphere.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Catalog Service Unit Tests")
class CatalogServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private CatalogService catalogService;

    private Category testCategory;
    private Product testProduct;
    private CreateProductRequest createProductRequest;
    private CreateCategoryRequest createCategoryRequest;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic products")
                .isActive(true)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .description("Apple iPhone 15 128GB")
                .price(new BigDecimal("79999.00"))
                .originalPrice(new BigDecimal("89999.00"))
                .stockQuantity(50)
                .category(testCategory)
                .isActive(true)
                .isFeatured(true)
                .build();

        createProductRequest = new CreateProductRequest();
        createProductRequest.setName("iPhone 15");
        createProductRequest.setDescription("Apple iPhone 15 128GB");
        createProductRequest.setPrice(new BigDecimal("79999.00"));
        createProductRequest.setOriginalPrice(new BigDecimal("89999.00"));
        createProductRequest.setStockQuantity(50);
        createProductRequest.setCategoryId(1L);
        createProductRequest.setFeatured(true);

        createCategoryRequest = new CreateCategoryRequest();
        createCategoryRequest.setName("Electronics");
        createCategoryRequest.setDescription("Electronic products");
    }

    // ─── Get Product Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("Get Product By Id — success")
    void getProductById_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));

        ApiResponse<ProductDTO> response = catalogService.getProductById(1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("iPhone 15");
        assertThat(response.getData().getPrice())
                .isEqualByComparingTo(new BigDecimal("79999.00"));
    }

    @Test
    @DisplayName("Get Product By Id — throws ResourceNotFoundException")
    void getProductById_notFound_throwsException() {
        when(productRepository.findByIdAndIsActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Get Featured Products Tests ─────────────────────────────────────────

    @Test
    @DisplayName("Get Featured Products — returns featured list")
    void getFeaturedProducts_success() {
        when(productRepository.findByIsFeaturedTrueAndIsActiveTrue())
                .thenReturn(List.of(testProduct));

        ApiResponse<List<ProductDTO>> response =
                catalogService.getFeaturedProducts();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getName()).isEqualTo("iPhone 15");
    }

    // ─── Create Product Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Create Product — success")
    void createProduct_success() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        ApiResponse<ProductDTO> response =
                catalogService.createProduct(createProductRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("iPhone 15");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Create Product — throws ResourceNotFoundException for invalid category")
    void createProduct_invalidCategory_throwsException() {
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                catalogService.createProduct(createProductRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    // ─── Delete Product Tests ────────────────────────────────────────────────

    @Test
    @DisplayName("Delete Product — soft deletes product")
    void deleteProduct_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        ApiResponse<Void> response = catalogService.deleteProduct(1L);

        assertThat(response.isSuccess()).isTrue();
        verify(productRepository).save(any(Product.class));
        assertThat(testProduct.isActive()).isFalse();
    }

    @Test
    @DisplayName("Delete Product — throws ResourceNotFoundException")
    void deleteProduct_notFound_throwsException() {
        when(productRepository.findByIdAndIsActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.deleteProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    // ─── Update Stock Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("Update Stock — success")
    void updateStock_success() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);

        ApiResponse<ProductDTO> response = catalogService.updateStock(1L, 100);

        assertThat(response.isSuccess()).isTrue();
        assertThat(testProduct.getStockQuantity()).isEqualTo(100);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Update Stock — throws BadRequestException for negative quantity")
    void updateStock_negativeQuantity_throwsBadRequest() {
        when(productRepository.findByIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> catalogService.updateStock(1L, -5))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("negative");
    }

    // ─── Category Tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Create Category — success")
    void createCategory_success() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(testCategory);

        ApiResponse<CategoryDTO> response =
                catalogService.createCategory(createCategoryRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getName()).isEqualTo("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Create Category — throws ConflictException for duplicate name")
    void createCategory_duplicateName_throwsConflict() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                catalogService.createCategory(createCategoryRequest))
                .isInstanceOf(ConflictException.class);

        verify(categoryRepository, never()).save(any());
    }

    // ─── Get All Categories Tests ────────────────────────────────────────────

    @Test
    @DisplayName("Get All Categories — returns active categories")
    void getAllCategories_success() {
        when(categoryRepository.findByIsActiveTrue())
                .thenReturn(List.of(testCategory));

        ApiResponse<List<CategoryDTO>> response =
                catalogService.getAllCategories();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getName()).isEqualTo("Electronics");
    }

    // ─── Get Products By Category Tests ──────────────────────────────────────

    @Test
    @DisplayName("Get Products By Category — throws ResourceNotFoundException for invalid category")
    void getProductsByCategory_invalidCategory_throwsException() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() ->
                catalogService.getProductsByCategory(99L, 0, 12))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Get Products By Category — success")
    void getProductsByCategory_success() {
        Page<Product> page = new PageImpl<>(List.of(testProduct));
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByCategoryIdAndIsActiveTrue(
                eq(1L), any(Pageable.class))).thenReturn(page);

        var response = catalogService.getProductsByCategory(1L, 0, 12);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().getContent()).hasSize(1);
    }
}